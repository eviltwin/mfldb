(function() {
    require.config({
        baseUrl: "codemirror:"
    });

    require(["lib/codemirror", "mode/clike/clike", "addon/display/fullscreen"], requireFinished, requireError);

    function requireFinished(CodeMirror) {
        var codemirror = CodeMirror(document.body, {
            lineNumbers: true,
            mode: "text/x-java",
            fullScreen: true,
            readOnly: true
        });

        shim.setCodeMirrorObject(codemirror);
    }

    function requireError(err) {
        shim.logError("RequireJS encountered an error!");
    }
})();
