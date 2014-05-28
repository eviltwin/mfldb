package uk.ac.imperial.doc.mfldb.bridge.mockvm;

import com.google.common.collect.ImmutableList;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.event.ClassPrepareEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Represents a fake class that has been loaded into a mocked VirtualMachine instance.
 */
public class TestClass {

    /**
     * Length in lines of the source file corresponding to this class.
     */
    public final int nLines;

    /**
     * The fully-qualified name of this class.
     */
    public final String name;

    /**
     * The mocked ReferenceType instance corresponding to this class.
     */
    protected final ReferenceType referenceType;

    /**
     * A mapping of source lines to mocked Location instances for setting and verifying breakpoints.
     */
    private final Map<Integer, List<Location>> lineMapping = new HashMap<>();

    /**
     * Constructs a new TestClass instance.
     * @param name The fully-qualified name of the fake class to be represented.
     * @param nLines The length in lines of the source file corresponding to this class.
     */
    protected TestClass(String name, int nLines) {
        this.nLines = nLines;
        this.name = name;

        // Mock out corresponding ReferenceType
        referenceType = mock(ReferenceType.class);
        when(referenceType.name()).thenReturn(name);
        when(referenceType.isPrepared()).thenReturn(false);
        try {
            when(referenceType.locationsOfLine(anyInt())).then(invocation -> {
                Integer line = (Integer) invocation.getArguments()[0];

                if (line > nLines) {
                    return Collections.emptyList();
                }

                List<Location> result = lineMapping.get(line);
                if (result == null) {
                    Location location = mock(Location.class);
                    when(location.method()).thenReturn(mock(Method.class));
                    result = ImmutableList.of(location);
                    lineMapping.put(line, result);
                }
                return result;
            });
        } catch (AbsentInformationException e) {
            // This should never, ever happen. If it does, throw as RuntimeException so that the test fails.
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetch the previously mocked Location corresponding to a given line in this class's source file.
     *
     * Note: This will be the empty list if the specified line was never requested from the corresponding ReferenceType
     * @param line The line number to fetch the Locations of.
     * @return A list of Locations.
     */
    public List<Location> locationsOfLine(int line) {
        return lineMapping.getOrDefault(line, Collections.emptyList());
    }

    /**
     * Fakes the preparation of this class in the mocked VirtualMachine.
     *
     * This means the corresponding ReferenceType will be retrievable from the parent mocked VirtualMachine.
     * @return A mocked ClassPrepareEvent to be passed to any object under test.
     */
    public ClassPrepareEvent makePrepared() {
        if (referenceType.isPrepared()) {
            throw new IllegalStateException("TestClass(\"%s\", %i) has already been prepared");
        }
        when(referenceType.isPrepared()).thenReturn(true);

        // Create a phony ClassPrepareEvent. Sadly this won't be associated with any ClassPrepareRequest...
        ClassPrepareEvent event = mock(ClassPrepareEvent.class);
        when(event.referenceType()).thenReturn(referenceType);

        return event;
    }
}
