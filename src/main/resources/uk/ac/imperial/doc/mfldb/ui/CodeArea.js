(function() {
    require.config({
        baseUrl: "codemirror:"
    });

    require(["lib/codemirror", "mode/clike/clike", "addon/display/fullscreen"], requireFinished, requireError);

    function requireFinished(CodeMirror) {
        var codemirror = CodeMirror(document.body, {
            lineNumbers: true,
            gutters: ["CodeMirror-linenumbers", "breakpoints"],
            mode: "text/x-java",
            fullScreen: true,
            readOnly: true
        });

        codemirror.on("gutterClick", function(cm, n) {
            shim.toggleBreakpoint(n + 1);
        });

        codemirror.markBreakpoint = function(n, marked) {
            codemirror.setGutterMarker(n - 1, "breakpoints", marked ? makeMarker() : null);
        }

        shim.setCodeMirrorObject(codemirror);
    }

    function requireError(err) {
        shim.logError("RequireJS encountered an error!");
    }

    function makeMarker() {
      var marker = document.createElement("img");
      marker.src = "db_set_breakpoint.png"
      return marker;
    }
})();
