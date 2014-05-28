package uk.ac.imperial.doc.mfldb.bridge.mockvm;

import com.sun.jdi.request.ClassPrepareRequest;

/**
 * Event which represents the creation of a BreakpointRequest.
 */
public class CreateClassPrepareRequestEvent implements Event {

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
