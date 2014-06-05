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

        codemirror.markBreakpoint = function(n, src) {
            codemirror.setGutterMarker(n - 1, "breakpoints", makeMarker(src));
        }

        codemirror.clearBreakpoint = function(n) {
            codemirror.setGutterMarker(n - 1, "breakpoints", null);
        }

        shim.setCodeMirrorObject(codemirror);
    }

    function requireError(err) {
        shim.logError("RequireJS encountered an error!");
    }

    function makeMarker(src) {
      var marker = document.createElement("img");
      marker.src = src;
      return marker;
    }
})();
