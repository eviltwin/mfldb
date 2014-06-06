package uk.ac.imperial.doc.mfldb.bridge;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.*;

/**
 * Classifies incoming JDI Events and dispatches them to {@link java.util.EventListener}s
 */
public class EventThread extends Thread {

    private final VirtualMachine vm;
    private Callbacks callbacks = null;

    private boolean connected = true;
    private boolean vmDied = true;

    EventThread(VirtualMachine vm) {
        super("JDI Event Dispatch");
        this.vm = vm;
    }

    /**
     * Run the event handling thread.
     * As long as we are connected, get event sets off
     * the queue and dispatch the events within them.
     */
    @Override
    public void run() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                Callbacks callbacks = this.callbacks;
                if (callbacks != null) {
                    callbacks.eventSet(eventSet);
                }
                for (Event event : eventSet) {
                    handleEvent(event);
                }
            } catch (InterruptedException exc) {
                // Ignore
            } catch (VMDisconnectedException discExc) {
                handleDisconnectedException();
                break;
            }
        }
    }

    /**
     * Dispatch incoming events
     */
    private void handleEvent(Event event) {
        Callbacks callbacks = this.callbacks;
        if (callbacks != null) {
            if (event instanceof ClassPrepareEvent) {
                callbacks.classPrepareEvent((ClassPrepareEvent) event);
            } else if (event instanceof BreakpointEvent) {
                callbacks.breakpointEvent((BreakpointEvent) event);
            } else if (event instanceof VMStartEvent) {
                callbacks.vmStartEvent((VMStartEvent) event);
            } else if (event instanceof VMDeathEvent) {
                vmDied = true;
                callbacks.vmDeathEvent((VMDeathEvent) event);
            } else if (event instanceof VMDisconnectEvent) {
                connected = false;
                callbacks.vmDisconnectEvent((VMDisconnectEvent) event);
            } else {
                throw new Error("Unexpected event type");
            }
        }
    }

    /**
     * A VMDisconnectedException has happened while dealing with
     * another event. We need to flush the event queue, dealing only
     * with exit events (VMDeath, VMDisconnect) so that we terminate
     * correctly.
     */
    synchronized void handleDisconnectedException() {
        EventQueue queue = vm.eventQueue();
        while (connected) {
            try {
                EventSet eventSet = queue.remove();
                for (Event event : eventSet) {
                    if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                        handleEvent(event);
                    }
                }
            } catch (InterruptedException exc) {
                // ignore
            }
        }
    }

    public void setCallbacks(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Callbacks for the EventThread to its owner.
     */
    public static interface Callbacks {
        void eventSet(EventSet events);
        void vmStartEvent(VMStartEvent event);
        void classPrepareEvent(ClassPrepareEvent event);
        void breakpointEvent(BreakpointEvent event);
        void vmDeathEvent(VMDeathEvent event);
        void vmDisconnectEvent(VMDisconnectEvent event);
    }
}
