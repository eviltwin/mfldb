package uk.ac.imperial.doc.mfldb.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Implementation of {@link URLStreamHandler} which allows accessing resources at a given basepath.
 */
public class ResourceURLStreamHandler extends URLStreamHandler {

    /**
     * Basepath for all resource files served by this handler.
     */
    private final String basepath;

    /**
     * Constructs a new URLStreamHandler instance.
     *
     * @param basepath prefix applied to all paths served by this handler.
     */
    public ResourceURLStreamHandler(String basepath) {
        this.basepath = basepath;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String path = u.getPath();
        URL url = getClass().getResource(basepath + (path.startsWith("/") ? path.substring(1) : path));
        return url.openConnection();
    }
}
