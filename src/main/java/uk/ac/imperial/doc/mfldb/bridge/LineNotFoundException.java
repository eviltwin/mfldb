package uk.ac.imperial.doc.mfldb.bridge;

/**
 * Created by graham on 22/05/14.
 */
public class LineNotFoundException extends Exception {

    public LineNotFoundException(String className, int lineNumber) {
        super(String.format("Could not find line %d in %s", lineNumber, className));
    }
}
