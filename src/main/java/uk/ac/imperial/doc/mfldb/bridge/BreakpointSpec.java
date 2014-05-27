package uk.ac.imperial.doc.mfldb.bridge;

/**
 * Created by graham on 21/05/14.
 */
public class BreakpointSpec {

    public final String className;
    public final int lineNumber;

    public BreakpointSpec(String className, int lineNumber) {
        this.className = className;
        this.lineNumber = lineNumber;
    }
}
