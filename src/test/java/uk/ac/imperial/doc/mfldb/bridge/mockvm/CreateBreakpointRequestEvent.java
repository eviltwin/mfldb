package uk.ac.imperial.doc.mfldb.bridge.mockvm;

import com.sun.jdi.request.BreakpointRequest;

/**
 * Event which represents the creation of a BreakpointRequest.
 */
public class CreateBreakpointRequestEvent implements Event {

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
