package uk.ac.imperial.doc.mfldb.bridge;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manages deferral and resolution of pending breakpoints.
 */
class BreakpointManager {

    /**
     * Map from class name to specification of deferred breakpoints.
     */
    private final Multimap<String, BreakpointSpec> deferredBreakpoints = ArrayListMultimap.create();

    /**
     * Map from class name to ClassPrepareRequest
     */
    private final Map<String, ClassPrepareRequest> classPrepareRequests = new HashMap<>();

    /**
     * Map from BreakpointSpec to the resolved BreakpointRequest
     */
    private final Map<BreakpointSpec, BreakpointRequest> resolvedBreakpoints = new HashMap<>();

    /**
     * The VirtualMachine for which this object is managing the breakpoints of.
     */
    private final VirtualMachine vm;

    /**
     * The callback to be invoked if resolution of a breakpoint succeeds.
     */
    private Consumer<BreakpointSpec> resolutionSuccessCallback;

    /**
     * The callback to be invoked if resolution of a breakpoint fails.
     */
    private BiConsumer<BreakpointSpec, Exception> resolutionFailureCallback;

    /**
     * Constructs a new BreakpointManager wrapping a given VirtualMachine.
     *
     * @param vm The VirtualMachine to be managed.
     */
    BreakpointManager(VirtualMachine vm) {
        this.vm = vm;
    }

    /**
     * Adds a breakpoint to the VirtualMachine.
     * <p>
     * The actual creation of the breakpoint may be deferred if the class in question has not yet been prepared by the
     * VirtualMachine.
     *
     * @param spec The specification of the breakpoint to be added.
     */
    void addBreakpoint(BreakpointSpec spec) {
        try {
            BreakpointRequest breakpointRequest = createBreakpointRequest(spec);
            if (breakpointRequest == null) {
                // Could not create the request, defer
                if (!deferredBreakpoints.containsKey(spec.className)) {
                    classPrepareRequests.put(spec.className, createClassPrepareRequest(spec));
                }
                deferredBreakpoints.put(spec.className, spec);
            } else {
                resolvedBreakpoints.put(spec, breakpointRequest);
                if (resolutionSuccessCallback != null) {
                    resolutionSuccessCallback.accept(spec);
                }
            }
        } catch (LineNotFoundException | AbsentInformationException e) {
            if (resolutionFailureCallback != null) {
                resolutionFailureCallback.accept(spec, e);
            }
        }
    }

    /**
     * Removes a breakpoint from the VirtualMachine
     *
     * This may delete a breakpoint which is still deferred and prevent it ever being resolved, or may deleted a
     * breakpoint
     * @param spec
     */
    public void removeBreakpoint(BreakpointSpec spec) {
        // If it's been resolved...
        BreakpointRequest breakpointRequest = resolvedBreakpoints.remove(spec);
        if (breakpointRequest != null) {
            vm.eventRequestManager().deleteEventRequest(breakpointRequest);
        }

        // If it's still deferred...
        deferredBreakpoints.remove(spec.className, spec);
        if (!deferredBreakpoints.containsKey(spec.className) && classPrepareRequests.containsKey(spec.className)) {
            ClassPrepareRequest classPrepareRequest = classPrepareRequests.remove(spec.className);
            vm.eventRequestManager().deleteEventRequest(classPrepareRequest);
        }
    }

    /**
     * Resolves any deferred breakpoints waiting on this class.
     *
     * @param event Event describing which class should have its deferred breakpoints added.
     */
    public void resolveDeferred(ClassPrepareEvent event) {
        Collection<BreakpointSpec> specs = deferredBreakpoints.removeAll(event.referenceType().name());
        if (specs != null && !specs.isEmpty()) {
            for (BreakpointSpec spec : specs) {
                try {
                    resolvedBreakpoints.put(spec, createBreakpointRequest(spec));
                    if (resolutionSuccessCallback != null) {
                        resolutionSuccessCallback.accept(spec);
                    }
                } catch (LineNotFoundException | AbsentInformationException e) {
                    if (resolutionFailureCallback != null) {
                        resolutionFailureCallback.accept(spec, e);
                    }
                }
            }
            classPrepareRequests.remove(event.referenceType().name());
            vm.resume();
        }
    }

    /**
     * Sets the callback to be invoked if resolution of a breakpoint succeeds.
     * @param resolutionSuccessCallback
     */
    public void setResolutionSuccessCallback(Consumer<BreakpointSpec> resolutionSuccessCallback) {
        this.resolutionSuccessCallback = resolutionSuccessCallback;
    }

    /**
     * Sets the callback to be invoked if resolution of a breakpoint fails.
     * @param resolutionFailureCallback
     */
    public void setResolutionFailureCallback(BiConsumer<BreakpointSpec, Exception> resolutionFailureCallback) {
        this.resolutionFailureCallback = resolutionFailureCallback;
    }

    /**
     * Helper to create a ClassPreparedRequest for a breakpoint being deferred.
     *
     * @param spec The breakpoint being deferred.
     * @return The created ClassPreparedRequest.
     */
    private ClassPrepareRequest createClassPrepareRequest(BreakpointSpec spec) {
        ClassPrepareRequest request = vm.eventRequestManager().createClassPrepareRequest();
        request.addClassFilter(spec.className);
        request.addCountFilter(1);
        request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        request.enable();
        return request;
    }

    /**
     * Helper to create a BreakpointRequest for a breakpoint being (potentially) resolved.
     *
     * @param spec The breakpoint being resolved.
     * @return The created BreakpointRequest or null if deferral is necessary.
     * @throws LineNotFoundException      If the line specified in the breakpoint couldn't be found.
     * @throws AbsentInformationException If the VirtualMachine didn't have the required information.
     */
    private BreakpointRequest createBreakpointRequest(BreakpointSpec spec) throws LineNotFoundException, AbsentInformationException {
        ReferenceType refType = vm.classesByName(spec.className).stream()
                .filter(t -> t.isPrepared())
                .findAny().orElse(null);

        if (refType == null) {
            return null;
        }

        List<Location> locations = refType.locationsOfLine(spec.lineNumber);
        if (locations.size() == 0) {
            throw new LineNotFoundException(spec.className, spec.lineNumber);
        }
        Location location = locations.get(0);
        if (location.method() == null) {
            throw new LineNotFoundException(spec.className, spec.lineNumber);
        }
        BreakpointRequest request = vm.eventRequestManager().createBreakpointRequest(location);
        request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        request.enable();
        return request;
    }
}
