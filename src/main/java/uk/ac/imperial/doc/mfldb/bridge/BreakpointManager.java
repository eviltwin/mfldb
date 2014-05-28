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
import java.util.List;

/**
 * Manages deferral and resolution of pending breakpoints.
 */
class BreakpointManager {

    /**
     * Map from class name to specification of deferred breakpoints.
     */
    private final Multimap<String, BreakpointSpec> deferredBreakpoints = ArrayListMultimap.create();

    /**
     * The VirtualMachine for which this object is managing the breakpoints of.
     */
    private final VirtualMachine vm;

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
     * @throws LineNotFoundException
     * @throws AbsentInformationException
     */
    void addBreakpoint(BreakpointSpec spec) throws LineNotFoundException, AbsentInformationException {
        BreakpointRequest breakpointRequest = createBreakpointRequest(spec);
        if (breakpointRequest == null) {
            // Could not create the request, defer
            if (!deferredBreakpoints.containsKey(spec.className)) {
                createClassPrepareRequest(spec);
            }
            deferredBreakpoints.put(spec.className, spec);
        }
    }

    /**
     * Resolves any deferred breakpoints waiting on this class.
     *
     * @param event Event describing which class should have its deferred breakpoints added.
     * @throws LineNotFoundException      If the line specified in the breakpoint couldn't be found.
     * @throws AbsentInformationException If the VirtualMachine didn't have the required information.
     */
    public void resolveDeferred(ClassPrepareEvent event) throws LineNotFoundException, AbsentInformationException {
        Collection<BreakpointSpec> specs = deferredBreakpoints.removeAll(event.referenceType().name());
        if (specs != null && !specs.isEmpty()) {
            for (BreakpointSpec spec : specs) {
                createBreakpointRequest(spec);
            }
            vm.resume();
        }
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
