package uk.ac.imperial.doc.mfldb.ui;

import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import static uk.ac.imperial.doc.mfldb.ui.Const.CODEAREA_HTML;

/**
 * Wraps a {@link WebView} for use with CodeMirror.
 */
public class CodeAreaController {

    private final WebView webView;
    private final Shim shim = new Shim();

    public CodeAreaController(WebView webView) {
        this.webView = webView;

        // Attach the shim to the WebView's "window" object, making it a global variable in JavaScript.
        JSObject window = (JSObject) webView.getEngine().executeScript("window");
        window.setMember("shim", shim);

        // Load the editor, it will then register itself with the shim.
        webView.getEngine().load(getClass().getResource(CODEAREA_HTML).toExternalForm());
    }

    public void replaceText(String text) {
        shim.replaceText(text);
    }

    protected class Shim {
        private JSObject codemirror;

        public void logError(String message) {
            System.err.println(message);
        }

        public void setCodeMirrorObject(JSObject codemirror) {
            this.codemirror = codemirror;
        }

        public void replaceText(String text) {
            codemirror.call("setValue", new Object[]{text});
        }
    }
}
