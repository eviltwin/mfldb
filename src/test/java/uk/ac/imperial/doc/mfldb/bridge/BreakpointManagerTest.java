package uk.ac.imperial.doc.mfldb.bridge;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.request.EventRequest;
import org.junit.Test;
import uk.ac.imperial.doc.mfldb.bridge.mockvm.Event;
import uk.ac.imperial.doc.mfldb.bridge.mockvm.MockVM;
import uk.ac.imperial.doc.mfldb.bridge.mockvm.TestClass;

import java.util.function.Consumer;

import static org.mockito.Mockito.verify;
import static org.truth0.Truth.ASSERT;
import static uk.ac.imperial.doc.mfldb.bridge.mockvm.MockVM.vmResumed;

/**
 * Tests for the BreakpointManager class.
 * <p>
 * Tests generally exercise the BreakpointManager's deferral of creating a BreakpointRequest until the class in question
 * has been loaded. Such deferral is achieved by creating a ClassPrepareRequest for the respective class and inserting
 * the deferred breakpoint when the corresponding ClassPrepareEvent is received. Only one such ClassPrepareRequest
 * should be created per class with pending breakpoints (ie an extra request shouldn't be created if one is already
 * pending and a second breakpoint is added (and deferred) for that same class.
 * <p>
 * Verification of proper deferral is done with the {@link #createdClassPrepareRequest(String)} method and verification
 * of resolved BreakpointRequests is done with {@link #createdBreakpointRequest(uk.ac.imperial.doc.mfldb.bridge.mockvm.TestClass, BreakpointSpec)}
 */
public class BreakpointManagerTest {

    /**
     * Helps to create a mocked VirtualMachine for the BreakpointManager under test to wrap.
     */
    private MockVM mockVM = new MockVM();

    /**
     * The BreakpointManager under test.
     */
    private BreakpointManager manager = new BreakpointManager(mockVM.getVirtualMachine());

    /**
     * Verifies that a BreakpointRequest was created correctly for the given TestClass and BreakpointSpec.
     *
     * @param testClass The test class returned from the MockVM, corresponding to the class named in the spec.
     * @param spec      The BreakpointSpec which was given to the BreakpointManager to cause this event.
     */
    private static Consumer<Event> createdBreakpointRequest(TestClass testClass, BreakpointSpec spec) {
        return MockVM.createdBreakpointRequest(request -> {
            Location location = testClass.locationsOfLine(spec.lineNumber).get(0);
            ASSERT.that(request.location()).isEqualTo(location);
        });
    }

    /**
     * Verifies that a ClassPrepareRequest was created correctly to defer creation of breakpoints for the named class.
     *
     * @param className The name of the class this request should be deferring breakpoints for.
     */
    private static Consumer<Event> createdClassPrepareRequest(String className) {
        return MockVM.createdClassPrepareRequest(request -> {
            verify(request).addClassFilter(className);
            verify(request).addCountFilter(1);
            verify(request).setSuspendPolicy(EventRequest.SUSPEND_ALL);
            verify(request).enable();
        });
    }

    /**
     * Tests that the BreakpointManager will defer the addition of a breakpoint for a class that isn't loaded yet.
     */
    @Test
    public void defersUntilLater() throws LineNotFoundException, AbsentInformationException {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec = new BreakpointSpec(c.name, 67);

        // When
        manager.addBreakpoint(spec);

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name)
        );
    }

    /**
     * Tests the deferral and then resolution of a single breakpoint.
     */
    @Test
    public void defersAndResolves() throws LineNotFoundException, AbsentInformationException {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec = new BreakpointSpec(c.name, 67);

        // When
        manager.addBreakpoint(spec);
        manager.resolveDeferred(c.makePrepared());

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name),
                createdBreakpointRequest(c, spec),
                vmResumed()
        );
    }

    /**
     * Tests the deferral of two breakpoints and then their subsequent resolution.
     */
    @Test
    public void defersAndResolvesTwo() throws LineNotFoundException, AbsentInformationException {
        // Given
        TestClass c1 = mockVM.addTestClass("foo.bar.baz", 107);
        TestClass c2 = mockVM.addTestClass("foo.bar.qux", 204);
        BreakpointSpec spec1 = new BreakpointSpec(c1.name, 67);
        BreakpointSpec spec2 = new BreakpointSpec(c2.name, 45);

        // When
        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);

        manager.resolveDeferred(c1.makePrepared());
        manager.resolveDeferred(c2.makePrepared());

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c1.name),
                createdClassPrepareRequest(c2.name),
                createdBreakpointRequest(c1, spec1),
                vmResumed(),
                createdBreakpointRequest(c2, spec2),
                vmResumed()
        );
    }

    /**
     * Tests the deferral of two breakpoints and then their subsequent resolution in the opposite order.
     */
    @Test
    public void defersAndResolvesTwoReversed() throws LineNotFoundException, AbsentInformationException {
        // Given
        TestClass c1 = mockVM.addTestClass("foo.bar.baz", 107);
        TestClass c2 = mockVM.addTestClass("foo.bar.qux", 204);
        BreakpointSpec spec1 = new BreakpointSpec(c1.name, 67);
        BreakpointSpec spec2 = new BreakpointSpec(c2.name, 45);

        // When
        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);

        manager.resolveDeferred(c2.makePrepared());
        manager.resolveDeferred(c1.makePrepared());

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c1.name),
                createdClassPrepareRequest(c2.name),
                createdBreakpointRequest(c2, spec2),
                vmResumed(),
                createdBreakpointRequest(c1, spec1),
                vmResumed()
        );
    }
}
