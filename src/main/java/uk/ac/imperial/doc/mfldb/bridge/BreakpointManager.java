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
 * Created by graham on 23/05/14.
 */
class BreakpointManager {

    private final Multimap<String, BreakpointSpec> deferredBreakpoints = ArrayListMultimap.create();
    private final VirtualMachine vm;

    BreakpointManager(VirtualMachine vm) {
        this.vm = vm;
    }

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

    public void resolveDeferred(ClassPrepareEvent event) throws LineNotFoundException, AbsentInformationException {
        Collection<BreakpointSpec> specs = deferredBreakpoints.removeAll(event.referenceType().name());
        if (specs != null && !specs.isEmpty()) {
            for (BreakpointSpec spec : specs) {
                createBreakpointRequest(spec);
            }
            vm.resume();
        }
    }

    private ClassPrepareRequest createClassPrepareRequest(BreakpointSpec spec) {
        ClassPrepareRequest request = vm.eventRequestManager().createClassPrepareRequest();
        request.addClassFilter(spec.className);
        request.addCountFilter(1);
        request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        request.enable();
        return request;
    }

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
