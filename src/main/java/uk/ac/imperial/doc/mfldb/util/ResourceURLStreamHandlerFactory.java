package uk.ac.imperial.doc.mfldb.util;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import static uk.ac.imperial.doc.mfldb.util.Const.*;

/**
 * Implementation of {@link URLStreamHandlerFactory} for constructing {@link ResourceURLStreamHandler}s.
 */
public class ResourceURLStreamHandlerFactory implements URLStreamHandlerFactory {

    /**
     * URLStreamHandler instance for CodeMirror files.
     */
    private final URLStreamHandler codeMirrorHandler;

    /**
     * URLStreamHandler instance for RequireJS files.
     */
    private final URLStreamHandler requireJSHandler;

    /**
     * Injection constructor to be used by tests.
     *
     * @param codeMirrorHandler
     */
    protected ResourceURLStreamHandlerFactory(URLStreamHandler codeMirrorHandler, URLStreamHandler requireJSHandler) {
        this.codeMirrorHandler = codeMirrorHandler;
        this.requireJSHandler = requireJSHandler;
    }

    /**
     * Constructs a new ResourceURLStreamHandlerFactory instance.
     */
    public ResourceURLStreamHandlerFactory() {
        this(
                new ResourceURLStreamHandler(CODE_MIRROR_BASEPATH),
                new ResourceURLStreamHandler(REQUIRE_JS_BASEPATH)
        );
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equals(CODE_MIRROR_PROTOCOL)) {
            return codeMirrorHandler;
        } else if (protocol.equals(REQUIRE_JS_PROTOCOL)) {
            return requireJSHandler;
        } else {
            return null;
        }
    }
}
