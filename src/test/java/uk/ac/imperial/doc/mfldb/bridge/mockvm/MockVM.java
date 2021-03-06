package uk.ac.imperial.doc.mfldb.bridge.mockvm;

import com.sun.jdi.Location;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.truth0.Truth.ASSERT;

/**
 * Helper for creating a mocked VirtualMachine.
 * <p>
 * Functionality is limited to just what's needed for adequate testing.
 */
public class MockVM {

    /**
     * The mocked VirtualMachine object
     */
    private final VirtualMachine vm;

    /**
     * All TestClass instances created so far.
     */
    private final List<TestClass> testClasses = new ArrayList<>();

    /**
     * Event log to be verified at the end of testing.
     */
    private final List<Event> events = new ArrayList<>();

    /**
     * Default constructor.
     */
    public MockVM() {
        // Mock the VirtualMachine
        vm = mock(VirtualMachine.class);
        when(vm.allClasses()).thenReturn(Collections.unmodifiableList(testClasses.stream()
                .map(c -> c.referenceType)
                .filter(t -> t.isPrepared())
                .collect(Collectors.toList())));
        when(vm.classesByName(any())).then(invocation -> testClasses.stream()
                .map(c -> c.referenceType)
                .filter(t -> t.isPrepared())
                .filter(t -> t.name().equals(invocation.getArguments()[0]))
                .collect(Collectors.toList()));
        doAnswer(invocation -> events.add(new Event.VMResumeEvent())).when(vm).resume();

        // Mock the EventRequestManager
        EventRequestManager eventRequestManager = mock(EventRequestManager.class);
        when(vm.eventRequestManager()).thenReturn(eventRequestManager);
        when(eventRequestManager.createClassPrepareRequest()).then(invocation -> {
            ClassPrepareRequest request = mock(ClassPrepareRequest.class);
            events.add(new Event.CreateClassPrepareRequestEvent(request));
            return request;
        });
        when(eventRequestManager.createBreakpointRequest(any())).then(invocation -> {
            BreakpointRequest request = mock(BreakpointRequest.class);
            when(request.location()).thenReturn((Location) invocation.getArguments()[0]);
            events.add(new Event.CreateBreakpointRequestEvent(request));
            return request;
        });
        doAnswer(invocation -> {
            events.add(new Event.DeletedEventRequestEvent(((EventRequest) invocation.getArguments()[0])));
            return null;
        }).when(eventRequestManager).deleteEventRequest(any());
    }

    /**
     * Returns the mocked VirtualMachine object.
     *
     * @return the mocked VirtualMachine object.
     */
    public VirtualMachine getVirtualMachine() {
        return vm;
    }

    /**
     * Creates and returns a new mocked class with the given name and number of lines of code.
     * <p>
     * This can then in turn be used to generate mocked ClassPrepareEvents as well as allowing the VM to correctly list
     * all currently prepared classes.
     *
     * @param name  The fully qualified name of the class to be inserted into the mocked VirtualMachine.
     * @param lines The number of lines in the corresponding source file for this class.
     * @return
     */
    public TestClass addTestClass(String name, int lines) {
        TestClass testClass = new TestClass(name, lines);
        testClasses.add(testClass);
        return testClass;
    }

    /**
     * Verifies the event log with the provided EventVerifiers.
     * <p>
     * Further ensures that there exists precisely one event in the event log per Consumer in the provided verifiers.
     *
     * @param verifiers Var-args of Consumers to verify each event in turn.
     */
    @SafeVarargs
    public final void verifyEventLog(Consumer<Event>... verifiers) {
        ASSERT.withFailureMessage(String.format("MockVM event log has %d items but %d verifiers were provided", events.size(), verifiers.length))
                .that(verifiers.length).is(events.size());
        for (int i = 0; i < verifiers.length; i++) {
            verifiers[i].accept(events.get(i));
        }
    }
}
