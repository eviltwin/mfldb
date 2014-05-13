package uk.ac.imperial.doc.mfldb.bridge;

/**
 * Created by graham on 08/05/14.
 */
public class DebugSessionException extends Exception {

    public DebugSessionException(Throwable cause) {
        super(cause);
    }

    public DebugSessionException(String message) {
        super(message);
    }
}
