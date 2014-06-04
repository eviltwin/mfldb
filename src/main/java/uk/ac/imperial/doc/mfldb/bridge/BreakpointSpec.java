package uk.ac.imperial.doc.mfldb.bridge;

/**
 * Created by graham on 21/05/14.
 */
public final class BreakpointSpec {

    public final String className;
    public final int lineNumber;

    public BreakpointSpec(String className, int lineNumber) {
        this.className = className;
        this.lineNumber = lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreakpointSpec that = (BreakpointSpec) o;

        if (lineNumber != that.lineNumber) return false;
        if (!className.equals(that.className)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + lineNumber;
        return result;
    }
}
