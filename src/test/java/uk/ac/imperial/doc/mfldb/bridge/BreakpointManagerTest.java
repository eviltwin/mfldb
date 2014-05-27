package uk.ac.imperial.doc.mfldb.bridge;

import com.google.common.collect.ImmutableList;
import com.sun.jdi.*;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.truth0.Truth.ASSERT;

@RunWith(MockitoJUnitRunner.class)
public class BreakpointManagerTest {

    @Mock
    private VirtualMachine vm;

    private List<ReferenceType> referenceTypes = new ArrayList<>();

    @Mock
    private EventRequestManager eventRequestManager;

    private List<ClassPrepareRequest> classPrepareRequests = new ArrayList<>();
    private List<BreakpointRequest> breakpointRequests = new ArrayList<>();

    private BreakpointManager manager;

    @Before
    public void setup() {
        // Mock returning loaded reference types
        when(vm.allClasses()).thenReturn(Collections.unmodifiableList(referenceTypes));
        when(vm.classesByName(any())).then(invokation -> Collections.unmodifiableList(referenceTypes.stream()
                .filter(referenceType -> referenceType.name().equals(invokation.getArguments()[0]))
                .collect(Collectors.toList())));

        // Mock the event request manager
        when(vm.eventRequestManager()).thenReturn(eventRequestManager);
        when(eventRequestManager.createClassPrepareRequest()).then(invocation -> {
            ClassPrepareRequest request = mock(ClassPrepareRequest.class);
            classPrepareRequests.add(request);
            return request;
        });
        when(eventRequestManager.createBreakpointRequest(any())).then(invocation -> {
            BreakpointRequest request = mock(BreakpointRequest.class);
            when(request.location()).thenReturn((Location) invocation.getArguments()[0]);
            breakpointRequests.add(request);
            return request;
        });

        // Instantiate the manager here, once the vm has been appropriately mocked.
        manager = new BreakpointManager(vm);
    }

    @Test
    public void defersUntilLater() throws LineNotFoundException, AbsentInformationException {
        BreakpointSpec spec = new BreakpointSpec("foo.bar.baz", 67);

        manager.addBreakpoint(spec);

        // Verify that no BreakPointRequests were created yet and a correct ClassPrepareRequest was made
        ASSERT.that(breakpointRequests.size()).is(0);
        ASSERT.that(classPrepareRequests.size()).is(1);
        verifyClassPrepareRequest(spec, 0);

        // Resolve the pending breakpoint with a ClassPrepareEvent
        manager.resolveDeferred(mockPrepareClass(spec.className, 0));
        ASSERT.that(breakpointRequests.size()).is(1);
        verifyBreakpointRequest(spec, 0, 0);
        verifyAllResolved();
    }

    @Test
    public void defersTwoUntilLater() throws LineNotFoundException, AbsentInformationException {
        BreakpointSpec spec1 = new BreakpointSpec("foo.bar.baz", 67);
        BreakpointSpec spec2 = new BreakpointSpec("foo.bar.qux", 45);

        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);

        // Verify that no BreakPointRequests were created and the correct ClassPrepareRequests were made
        verify(eventRequestManager, never()).createBreakpointRequest(any());
        ASSERT.that(classPrepareRequests.size()).is(2);
        verifyClassPrepareRequest(spec1, 0);
        verifyClassPrepareRequest(spec2, 1);

        // Resolve the pending breakpoints with ClassPrepareEvents, in order
        manager.resolveDeferred(mockPrepareClass(spec1.className, 0));
        ASSERT.that(breakpointRequests.size()).is(1);
        verifyBreakpointRequest(spec1, 0, 0);

        manager.resolveDeferred(mockPrepareClass(spec2.className, 1));
        ASSERT.that(breakpointRequests.size()).is(2);
        verifyBreakpointRequest(spec2, 1, 1);

        verifyAllResolved();
    }

    @Test
    public void defersTwoUntilLaterReversed() throws LineNotFoundException, AbsentInformationException {
        BreakpointSpec spec1 = new BreakpointSpec("foo.bar.baz", 67);
        BreakpointSpec spec2 = new BreakpointSpec("foo.bar.qux", 45);

        manager.addBreakpoint(spec1);
        manager.addBreakpoint(spec2);

        // Verify that no BreakPointRequests were created and the correct ClassPrepareRequests were made
        verify(eventRequestManager, never()).createBreakpointRequest(any());
        ASSERT.that(classPrepareRequests.size()).is(2);
        verifyClassPrepareRequest(spec1, 0);
        verifyClassPrepareRequest(spec2, 1);

        // Resolve the pending breakpoints with ClassPrepareEvents, in order
        manager.resolveDeferred(mockPrepareClass(spec2.className, 1));
        ASSERT.that(breakpointRequests.size()).is(1);
        verifyBreakpointRequest(spec2, 0, 0);

        manager.resolveDeferred(mockPrepareClass(spec1.className, 0));
        ASSERT.that(breakpointRequests.size()).is(2);
        verifyBreakpointRequest(spec1, 1, 1);

        verifyAllResolved();
    }

    private ClassPrepareEvent mockPrepareClass(String name, int index) {
        ReferenceType type = mock(ReferenceType.class);
        when(type.name()).thenReturn(name);
        when(type.isPrepared()).thenReturn(true);
        try {
            Map<Integer, List<Location>> mapping = new HashMap<>();
            when(type.locationsOfLine(anyInt())).then(invocation -> {
                Integer line = (Integer) invocation.getArguments()[0];

                List<Location> result = mapping.get(line);
                if (result == null) {
                    Location location = mock(Location.class);
                    when(location.method()).thenReturn(mock(Method.class));
                    result = ImmutableList.of(location);
                    mapping.put(line, result);
                }
                return result;
            });
        } catch (AbsentInformationException e) {
            // This should never, ever happen. If it does, throw as RuntimeException so that the test fails.
            throw new RuntimeException(e);
        }
        referenceTypes.add(type);

        ClassPrepareEvent event = mock(ClassPrepareEvent.class);
        when(event.referenceType()).thenReturn(type);
        when(event.request()).thenReturn(classPrepareRequests.get(index));

        return event;
    }

    private void verifyBreakpointRequest(BreakpointSpec spec, int breakpointIndex, int referenceTypeIndex) throws AbsentInformationException {
        ReferenceType referenceType = referenceTypes.get(referenceTypeIndex);
        Location location = referenceType.locationsOfLine(spec.lineNumber).get(0);
        ASSERT.that(breakpointRequests.get(breakpointIndex).location()).isEqualTo(location);
    }

    private void verifyClassPrepareRequest(BreakpointSpec spec, int index) {
        ClassPrepareRequest request = classPrepareRequests.get(index);
        verify(request).addClassFilter(spec.className);
        verify(request).addCountFilter(1);
        verify(request).setSuspendPolicy(EventRequest.SUSPEND_ALL);
        verify(request).enable();
    }

    private void verifyAllResolved() {
        classPrepareRequests.forEach(request -> verify(eventRequestManager).deleteEventRequest(request));
        verify(vm, times(classPrepareRequests.size())).resume();
    }
}
