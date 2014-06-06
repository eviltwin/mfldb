package uk.ac.imperial.doc.mfldb.ui;

import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import uk.ac.imperial.doc.mfldb.packagetree.BreakpointType;

import java.util.function.Consumer;

import static uk.ac.imperial.doc.mfldb.ui.Const.CODEAREA_HTML;

/**
 * Wraps a {@link WebView} for use with CodeMirror.
 */
public class CodeAreaController {

    private final WebView webView;

    private final Shim shim = new Shim();

    private JSObject codemirror;

    private Consumer<Integer> breakpointToggleHandler;

    public CodeAreaController(WebView webView) {
        this.webView = webView;

        // Attach the shim to the WebView's "window" object, making it a global variable in JavaScript.
        JSObject window = (JSObject) webView.getEngine().executeScript("window");
        window.setMember("shim", shim);

        // Load the editor, it will then register itself with the shim.
        webView.getEngine().load(getClass().getResource(CODEAREA_HTML).toExternalForm());
    }

    public void replaceText(String text) {
        codemirror.call("setValue", new Object[]{text});
    }

    public void setBreakpointToggleHandler(Consumer<Integer> handler) {
        breakpointToggleHandler = handler;
    }

    public void markBreakpoint(int lineNo, BreakpointType type) {
        codemirror.call("markBreakpoint", new Object[]{lineNo, "db_set_breakpoint.png"});
    }

    public void markBreakpointResolved(int lineNo, BreakpointType type) {
        codemirror.call("markBreakpoint", new Object[]{lineNo, "db_verified_breakpoint.png"});
    }

    public void markBreakpointResolutionFailed(int lineNo, BreakpointType type) {
        codemirror.call("markBreakpoint", new Object[]{lineNo, "db_invalid_breakpoint.png"});
    }

    public void clearBreakpoint(int lineNo) {
        codemirror.call("clearBreakpoint", new Object[]{lineNo});
    }

    public void jumpToLine(int lineNo) {
        codemirror.call("jumpToLine", new Object[]{lineNo});
    }

    public void markCurrentLine(int lineNo) {
        codemirror.call("markCurrentLine", new Object[]{lineNo});
    }

    protected class Shim {

        public void logError(String message) {
            System.err.println(message);
        }

        public void setCodeMirrorObject(JSObject codemirror) {
            CodeAreaController.this.codemirror = codemirror;
        }

        public void toggleBreakpoint(int n) {
            if (breakpointToggleHandler != null) {
                breakpointToggleHandler.accept(n);
            }
        }
    }
}
