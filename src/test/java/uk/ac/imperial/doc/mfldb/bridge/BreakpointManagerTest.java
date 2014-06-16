package uk.ac.imperial.doc.mfldb.bridge;

import com.sun.jdi.Location;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import org.junit.Test;
import uk.ac.imperial.doc.mfldb.bridge.mockvm.Event;
import uk.ac.imperial.doc.mfldb.bridge.mockvm.MockVM;
import uk.ac.imperial.doc.mfldb.bridge.mockvm.TestClass;

import java.util.function.Consumer;

import static org.mockito.Mockito.verify;
import static org.truth0.Truth.ASSERT;
import static uk.ac.imperial.doc.mfldb.bridge.mockvm.Event.vmResumed;

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
        return Event.createdBreakpointRequest(request -> {
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
        return Event.createdClassPrepareRequest(request -> {
            verify(request).addClassFilter(className);
            verify(request).addCountFilter(1);
            verify(request).setSuspendPolicy(EventRequest.SUSPEND_ALL);
            verify(request).enable();
        });
    }

    /**
     * Verifies that a ClassPrepareRequest was deleted correctly when a deferred breakpoint was removed.
     *
     * @param className The name of the class this request should be deferring breakpoints for.
     */
    private static Consumer<Event> deletedClassPrepareRequest(String className) {
        return Event.deletedEventRequest(request -> {
            ASSERT.withFailureMessage("Expected instance of ClassPrepareRequest").that(request instanceof ClassPrepareRequest).isTrue();
            verify((ClassPrepareRequest) request).addClassFilter(className);
        });
    }

    private static Consumer<Event> deletedBreakpointRequest(TestClass testClass, BreakpointSpec spec) {
        return Event.deletedEventRequest(request -> {
            ASSERT.withFailureMessage("Expected instance of BreakpointRequest").that(request instanceof BreakpointRequest).isTrue();
            Location location = testClass.locationsOfLine(spec.lineNumber).get(0);
            ASSERT.that(((BreakpointRequest) request).location()).isEqualTo(location);
        });
    }

    /**
     * Tests that the BreakpointManager will defer the addition of a breakpoint for a class that isn't loaded yet.
     */
    @Test
    public void defersUntilLater() {
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
    public void defersAndResolves() {
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
     * Ensure that already resolved breakpoints aren't re-resolved.
     *
     * The rationale here is that a second ClassPrepareRequest could have come from elsewhere in application and so two
     * identical ClassPrepareEvents could make their way to the BreakpointManager.
     */
    @Test
    public void doesNotResolveTwice() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec = new BreakpointSpec(c.name, 67);

        // When
        manager.addBreakpoint(spec);
        ClassPrepareEvent event = c.makePrepared();
        manager.resolveDeferred(event);
        manager.resolveDeferred(event);

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name),
                createdBreakpointRequest(c, spec),
                vmResumed()
        );
    }

    /**
     * Tests the immediate resolution of a breakpoint for a class which has already been prepared.
     */
    @Test
    public void resolvesImmediately() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec = new BreakpointSpec(c.name, 67);

        // When
        c.makePrepared();
        manager.addBreakpoint(spec);

        // Then
        mockVM.verifyEventLog(
                createdBreakpointRequest(c, spec)
        );
    }

    /**
     * Tests the deferral of two breakpoints and then their subsequent resolution.
     */
    @Test
    public void defersAndResolvesTwo() {
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
    public void defersAndResolvesTwoReversed() {
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

    /**
     * Tests that only one ClassPrepareRequest is created for two breakpoints in the same unprepared class.
     */
    @Test
    public void defersTwoWithOneRequest() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec1 = new BreakpointSpec(c.name, 67);
        BreakpointSpec spec2 = new BreakpointSpec(c.name, 34);

        // When
        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name)
        );
    }

    /**
     * Tests that both deferred breakpoints for a single class are resolved once the class is prepared.
     */
    @Test
    public void defersAndResolvesTwoWithOneRequest() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec1 = new BreakpointSpec(c.name, 67);
        BreakpointSpec spec2 = new BreakpointSpec(c.name, 34);

        // When
        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);
        manager.resolveDeferred(c.makePrepared());

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name),
                createdBreakpointRequest(c, spec1),
                createdBreakpointRequest(c, spec2),
                vmResumed()
        );
    }

    /**
     * Tests that removing a breakpoint before it is resolved will cause the ClassPrepareRequest to be deleted too.
     */
    @Test
    public void removeDeferredDeletesRequest() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec1 = new BreakpointSpec(c.name, 67);
        BreakpointSpec spec2 = new BreakpointSpec(c.name, 67);

        // When
        manager.addBreakpoint(spec1);
        manager.removeBreakpoint(spec2);
        manager.resolveDeferred(c.makePrepared());

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name),
                deletedClassPrepareRequest(c.name)
        );
    }

    /**
     * Tests that removing one breakpoint before both are resolved will not prevent the second from being resolved.
     */
    @Test
    public void removeOneDeferredStillResolvesSecond() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec1 = new BreakpointSpec(c.name, 67);
        BreakpointSpec spec2 = new BreakpointSpec(c.name, 34);

        // When
        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);
        manager.removeBreakpoint(spec1);
        manager.resolveDeferred(c.makePrepared());

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name),
                createdBreakpointRequest(c, spec2),
                vmResumed()
        );
    }

    /**
     * Tests that removing a breakpoint after it is resolved will delete the breakpoint request.
     */
    @Test
    public void removeAfterResolved() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec1 = new BreakpointSpec(c.name, 67);

        // When
        manager.addBreakpoint(spec1);
        manager.resolveDeferred(c.makePrepared());
        manager.removeBreakpoint(spec1);

        // Then
        mockVM.verifyEventLog(
                createdClassPrepareRequest(c.name),
                createdBreakpointRequest(c, spec1),
                vmResumed(),
                deletedBreakpointRequest(c, spec1)
        );
    }

    /**
     * Tests that removing a breakpoint inserted after a class is already loaded works as expected.
     */
    @Test
    public void removeAfterInsertedWhenAlreadyResolved() {
        // Given
        TestClass c = mockVM.addTestClass("foo.bar.baz", 107);
        BreakpointSpec spec1 = new BreakpointSpec(c.name, 67);

        // When
        manager.resolveDeferred(c.makePrepared());
        manager.addBreakpoint(spec1);
        manager.removeBreakpoint(spec1);

        // Then
        mockVM.verifyEventLog(
                createdBreakpointRequest(c, spec1),
                deletedBreakpointRequest(c, spec1)
        );
    }
}
