package uk.ac.imperial.doc.mfldb.bridge.mockvm;

import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;

import java.util.function.Consumer;

import static org.truth0.Truth.ASSERT;

/**
 * DSL for describing events happening within the {@link MockVM}.
 */
public abstract class Event {

    private Event() {
        // Ensure only inner types are instances.
    }

    /**
     * Verify that the mock VirtualMachine was resumed at this point in the event log.
     *
     * @return A Consumer which to verify the Event.
     */
    public static Consumer<Event> vmResumed() {
        return event -> ASSERT.withFailureMessage(String.format("Found event %s when expecting VMResumeEvent", event.toString()))
                .that(event instanceof VMResumeEvent).isTrue();
    }

    /**
     * Verify that a BreakpointRequest was created at this point in the event log.
     *
     * @param callback A callback which can optionally verify the mocked BreakpointRequest
     * @return A Consumer which to verify the Event.
     */
    public static Consumer<Event> createdBreakpointRequest(Consumer<BreakpointRequest> callback) {
        return event -> {
            ASSERT.withFailureMessage(String.format("Found event %s when expecting CreateBreakpointRequestEvent", event.toString()))
                    .that(event instanceof CreateBreakpointRequestEvent).isTrue();
            callback.accept(((CreateBreakpointRequestEvent) event).request);
        };
    }

    /**
     * Verify that a ClassPrepareRequest was created at this point in the event log.
     *
     * @param callback A callback which can optionally verify the mocked ClassPrepareRequest
     * @return A Consumer which to verify the Event.
     */
    public static Consumer<Event> createdClassPrepareRequest(Consumer<ClassPrepareRequest> callback) {
        return event -> {
            ASSERT.withFailureMessage(String.format("Found event %s when expecting CreateClassPrepareRequestEvent", event.toString()))
                    .that(event instanceof CreateClassPrepareRequestEvent).isTrue();
            callback.accept(((CreateClassPrepareRequestEvent) event).request);
        };
    }

    /**
     * Verify that a ClassPrepareRequest was created at this point in the event log.
     *
     * @param callback A callback which can optionally verify the mocked ClassPrepareRequest
     * @return A Consumer which to verify the Event.
     */
    public static Consumer<Event> deletedEventRequest(Consumer<EventRequest> callback) {
        return event -> {
            ASSERT.withFailureMessage(String.format("Found event %s when expecting DeletedEventRequest", event.toString()))
                    .that(event instanceof DeletedEventRequestEvent).isTrue();
            callback.accept(((DeletedEventRequestEvent) event).request);
        };
    }

    /**
     * Event which represents the {@link com.sun.jdi.VirtualMachine#resume()} method being called.
     */
    public static class VMResumeEvent extends Event {
    }

    /**
     * Event which represents the deletion of an EventRequest.
     */
    public static class DeletedEventRequestEvent extends Event {

        /**
         * The EventRequest that was deleted.
         */
        public final EventRequest request;

        /**
         * Instantiates a new DeletedEventRequestEvent.
         *
         * @param request The request that was deleted.
         */
        public DeletedEventRequestEvent(EventRequest request) {
            this.request = request;
        }
    }

    /**
     * Event which represents the creation of a ClassPrepareRequest.
     */
    public static class CreateClassPrepareRequestEvent extends Event {

        /**
         * The ClassPrepareRequest that was created.
         */
        public final ClassPrepareRequest request;

        /**
         * Instantiates a new CreateClassPrepareRequestEvent.
         *
         * @param request The request that was created.
         */
        public CreateClassPrepareRequestEvent(ClassPrepareRequest request) {
            this.request = request;
        }
    }

    /**
     * Event which represents the creation of a BreakpointRequest.
     */
    public static class CreateBreakpointRequestEvent extends Event {

        /**
         * The BreakpointRequest that was created.
         */
        public final BreakpointRequest request;

        /**
         * Instantiates a new CreateBreakpointRequestEvent.
         *
         * @param request The request that was created.
         */
        public CreateBreakpointRequestEvent(BreakpointRequest request) {
            this.request = request;
        }
    }
}
