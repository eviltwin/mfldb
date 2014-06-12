package uk.ac.imperial.doc.mfldb.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URLStreamHandler;

import static org.truth0.Truth.ASSERT;

/**
 * Tests for the {@link ResourceURLStreamHandlerFactory} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceURLStreamHandlerFactoryTest {

    /**
     * Mocked handler for CodeMirror urls
     */
    @Mock URLStreamHandler codeMirrorHandler;

    /**
     * Mocked handler for RequireJS urls
     */
    @Mock URLStreamHandler requireJSHandler;

    /**
     * Mocked handler for D3.js urls
     */
    @Mock URLStreamHandler d3jsHandler;

    /**
     * Mocked handler for jQuery urls
     */
    @Mock URLStreamHandler jQueryHandler;

    /**
     * Mocked handler for jsPlumb urls
     */
    @Mock URLStreamHandler jsPlumbHandler;

    /**
     * The ResourceURLStreamHandlerFactory under test
     */
    ResourceURLStreamHandlerFactory factory;

    /**
     * Initialize the ResourceURLStreamHandlerFactory with mocked URLStreamHandlers
     */
    @Before
    public void initialize() {
        factory = new ResourceURLStreamHandlerFactory(codeMirrorHandler, requireJSHandler, d3jsHandler, jQueryHandler, jsPlumbHandler);
    }

    /**
     * Ensure that the correct URLStreamHandler is returned for URLs with the protocol "codemirror"
     */
    @Test
    public void shouldReturnCodeMirror() {
        // When
        URLStreamHandler returned = factory.createURLStreamHandler("codemirror");

        // Then
        ASSERT.that(returned).isEqualTo(codeMirrorHandler);
    }

    /**
     * Ensure that the correct URLStreamHandler is returned for URLs with the protocol "requirejs"
     */
    @Test
    public void shouldReturnRequireJS() {
        // When
        URLStreamHandler returned = factory.createURLStreamHandler("requirejs");

        // Then
        ASSERT.that(returned).isEqualTo(requireJSHandler);
    }

    /**
     * Ensure that the correct URLStreamHandler is returned for URLs with the protocol "d3js"
     */
    @Test
    public void shouldReturnD3js() {
        // When
        URLStreamHandler returned = factory.createURLStreamHandler("d3js");

        // Then
        ASSERT.that(returned).isEqualTo(d3jsHandler);
    }

    /**
     * Ensure that the correct URLStreamHandler is returned for URLs with the protocol "jquery"
     */
    @Test
    public void shouldReturnjQuery() {
        // When
        URLStreamHandler returned = factory.createURLStreamHandler("jquery");

        // Then
        ASSERT.that(returned).isEqualTo(jQueryHandler);
    }

    /**
     * Ensure that the correct URLStreamHandler is returned for URLs with the protocol "jsplumb"
     */
    @Test
    public void shouldReturnJsPlumb() {
        // When
        URLStreamHandler returned = factory.createURLStreamHandler("jsplumb");

        // Then
        ASSERT.that(returned).isEqualTo(jsPlumbHandler);
    }

    /**
     * Ensure that other common protocols return null.
     */
    @Test
    public void shouldReturnNull() {
        // When
        URLStreamHandler file = factory.createURLStreamHandler("file");
        URLStreamHandler http = factory.createURLStreamHandler("http");
        URLStreamHandler ftp  = factory.createURLStreamHandler("ftp");
        URLStreamHandler jar  = factory.createURLStreamHandler("jar");

        // Then
        ASSERT.that(file).isNull();
        ASSERT.that(http).isNull();
        ASSERT.that(ftp).isNull();
        ASSERT.that(jar).isNull();
    }
}
