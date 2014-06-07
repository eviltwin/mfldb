package uk.ac.imperial.doc.mfldb.bridge;

import com.hypirion.io.Pipe;
import com.hypirion.io.RevivableInputStream;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by graham on 08/05/14.
 */
public class DebugSession {

    public enum State {READY, RUNNING, SUSPENDED, TERMINATED}

    // Because uttering the words "fuck it, I'll just make this into a static" has never created any considerable
    // amount of technical debt ever...
    private static final RevivableInputStream inZombie = new RevivableInputStream(System.in);

    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(this, "state", State.READY);

    /**
     * VirtualMachine under inspection
     */
    private final VirtualMachine vm;

    private final ThreadManager threadManager;

    private final BreakpointManager breakpointManager;

    private Pipe inPipe;
    private Pipe errPipe;
    private Pipe outPipe;

    /**
     * Thread reading EventQueue from the remote vm and dispatching events
     */
    private EventThread eventThread = null;

    public DebugSession(String cmd) throws DebugSessionException {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = connectorArguments(connector, cmd);
        try {
            vm = connector.launch(arguments);
        } catch (IOException | IllegalConnectorArgumentsException | VMStartException e) {
            throw new DebugSessionException(e);
        }
        vm.setDebugTraceMode(VirtualMachine.TRACE_NONE);
        threadManager = new ThreadManager(vm);
        breakpointManager = new BreakpointManager(vm);
        redirectOutput();
        startEventThread();

        eventThread.setCallbacks(new EventThread.Callbacks() {
            @Override
            public void eventSet(EventSet events) {
                threadManager.updateCurrentThread(events);
            }

            @Override
            public void vmStartEvent(VMStartEvent event) {
                Platform.runLater(() -> state.set(State.RUNNING));
            }

            @Override
            public void classPrepareEvent(ClassPrepareEvent event) {
                breakpointManager.resolveDeferred(event);
            }

            @Override
            public void breakpointEvent(BreakpointEvent event) {
                if (event.request().suspendPolicy() == EventRequest.SUSPEND_ALL) {
                    Platform.runLater(() -> state.set(State.SUSPENDED));
                }
            }

            @Override
            public void stepEvent(StepEvent event) {
                Platform.runLater(() -> state.set(State.SUSPENDED));
            }

            @Override
            public void vmDeathEvent(VMDeathEvent event) {
                Platform.runLater(() -> state.set(State.TERMINATED));
            }

            @Override
            public void vmDisconnectEvent(VMDisconnectEvent event) {
                Platform.runLater(() -> state.set(State.TERMINATED));
            }
        });
    }

    public void addBreakpoint(BreakpointSpec spec) {
        breakpointManager.addBreakpoint(spec);
    }

    public void removeBreakpoint(BreakpointSpec spec) {
        breakpointManager.removeBreakpoint(spec);
    }

    public void setBreakpointResolutionSuccessCallback(Consumer<BreakpointSpec> breakpointResolutionSuccessCallback) {
        breakpointManager.setResolutionSuccessCallback(breakpointResolutionSuccessCallback == null ? null :
                spec -> Platform.runLater(() -> breakpointResolutionSuccessCallback.accept(spec)));
    }

    public void setBreakpointResolutionFailureCallback(BiConsumer<BreakpointSpec, Exception> breakpointResolutionFailureCallback) {
        breakpointManager.setResolutionFailureCallback(breakpointResolutionFailureCallback == null ? null :
                (spec, e) -> Platform.runLater(() -> breakpointResolutionFailureCallback.accept(spec, e)));
    }

    public void ensureEnded() {
        if (getState() != State.TERMINATED) {
            vm.exit(0);
        }
        try {
            vm.process().waitFor();
            eventThread.join();
            inZombie.kill();
            inPipe.stop();
            outPipe.stop();
            errPipe.stop();
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public void pause() {
        vm.suspend();
        state.set(State.SUSPENDED);
    }

    public void resume() {
        vm.resume();
        state.set(State.RUNNING);
    }

    public void stepOver(ThreadReference thread) {
        clearPreviousStepRequest(thread);
        StepRequest request = vm.eventRequestManager().createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
        request.addCountFilter(1);
        request.enable();
        resume();
    }

    public void stepInto(ThreadReference thread) {
        clearPreviousStepRequest(thread);
        StepRequest request = vm.eventRequestManager().createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        request.addCountFilter(1);
        request.enable();
        resume();
    }

    public void stepOut(ThreadReference thread) {
        clearPreviousStepRequest(thread);
        StepRequest request = vm.eventRequestManager().createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OUT);
        request.addCountFilter(1);
        request.enable();
        resume();
    }

    public State getState() {
        return state.get();
    }

    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public ThreadReference getCurrentThread() {
        return threadManager.getCurrentThread();
    }

    public boolean isTerminated() {
        return getState() == State.TERMINATED;
    }

    /**
     * Return the launching connector's arguments.
     */
    private Map<String, Connector.Argument> connectorArguments(Connector connector, String cmd) throws DebugSessionException {
        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        Connector.Argument mainArg = arguments.get("main");
        if (mainArg == null) {
            throw new DebugSessionException("Bad launching connector");
        }
        mainArg.setValue(cmd);

        Connector.Argument optionArg = arguments.get("options");
        if (optionArg == null) {
            throw new DebugSessionException("Bad launching connector");
        }
        optionArg.setValue("-cp .");
        return arguments;
    }

    private void redirectOutput() {
        Process process = vm.process();
        inZombie.resurrect();

        inPipe = new Pipe(inZombie, process.getOutputStream());
        errPipe = new Pipe(process.getErrorStream(), System.err);
        outPipe = new Pipe(process.getInputStream(), System.out);

        inPipe.start();
        errPipe.start();
        outPipe.start();
    }

    private void startEventThread() {
        eventThread = new EventThread(vm);
        eventThread.start();
    }

    private void clearPreviousStepRequest(ThreadReference thread) {
        EventRequestManager manager = vm.eventRequestManager();
        manager.stepRequests().stream()
                .filter(r -> r.thread().equals(thread))
                .collect(Collectors.toList()) // avoid ConcurrentModificationException by storing in a list...
                .forEach(manager::deleteEventRequest);
    }
}
