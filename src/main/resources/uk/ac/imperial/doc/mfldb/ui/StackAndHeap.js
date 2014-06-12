require.config({
    paths: {
        "jquery": "jquery:jquery.min",
        "jsplumb": "http://jsplumbtoolkit.com/js/jquery.jsPlumb-1.6.2-min",
        "d3": "d3js:d3.min"
    }
});

require(["jquery", "jsplumb", "d3"], function($, jsplumb, d3) {
    // Monkey-patch jQuery onto the window for jsplumb's benefit...
    window.jQuery = $;

    deferredPointers = [];

    var jsShim = new function() {
        this.drawStackAndHeap = function(stack, heap) {
            drawStack(stack);
            drawHeap(heap);
            drawLinks();
        }
    }

    shim.registerShimObject(jsShim);

    jsplumb.ready(function() {
        // IMPORTANT: The styles below are *outright pinched* from python tutor and should be replaced...
        var brightRed = '#e93f34';

        var connectorBaseColor = '#005583';
        var connectorHighlightColor = brightRed;

        // We can have the body as the container, because we're just in a frame so who cares?
        jsPlumb.setContainer($("body"));

        jsplumb.importDefaults({
            Endpoint: ["Dot", {radius:3}],
            EndpointStyles: [{fillStyle: connectorBaseColor}, {fillstyle: null} /* make right endpoint invisible */],
            Anchors: ["RightMiddle", "LeftMiddle"],
            PaintStyle: {lineWidth: 1, strokeStyle: connectorBaseColor},

            // state machine curve style:
            Connector: [ "StateMachine" ],
            Overlays: [[ "Arrow", { length: 10, width:7, foldback:0.55, location:1 }]],
            EndpointHoverStyles: [{fillStyle: connectorHighlightColor}, {fillstyle: null} /* make right endpoint invisible */],
            HoverPaintStyle: {lineWidth: 1, strokeStyle: connectorHighlightColor},
        });
    });

    function drawStack(stack) {
        var frames = d3.select("#stack").selectAll(".frame").data(stack)
            .attr("id", function(object, i) {
                return "stack-frame-" + i;
            });

        frames.enter()
            .append("div")
            .attr("class", "frame")
            .attr("id", function(object, i) {
                return "stack-frame-" + i;
            })
            .call(function(div) {
                div.append("span").attr("class", "header");
                div.append("table");
            });

        frames.exit()
            .remove();

        var header = frames.select(".header")
            .datum(function(frame) {
                return frame.function;
            })
            .text(String);

        var variables = frames.select("table").selectAll("tr")
            .data(function(frame) {
                return frame.variables;
            })
            .call(fillVariableRow);

        variables.enter()
            .append("tr")
            .call(newVariableRow);

        variables.exit()
            .remove();
    }

    // Draw the heap
    function drawHeap(heap) {
        var objects = d3.select("#heap").selectAll(".object")
            .data(heap)
            .attr("id", function(object) {
                return "heap-object-" + object.id;
            });

        objects.enter()
            .append("div")
            .attr("class", "object")
            .attr("id", function(object) {
                return "heap-object-" + object.id;
            })
            .call(function(div) {
                div.append("span").attr("class", "header");
                div.append("span").attr("class", "id");
                div.append("table");
            });

        objects.exit()
            .remove();

        var header = objects.select(".header")
            .datum(function(object) {
                return object.klass;
            })
            .text(String);

        var id = objects.select(".id")
            .datum(function(object) {
                return object.id;
            })
            .text(function(s) {
                return "(id=" + s +")";
            });

        var fields = objects.select("table").selectAll("tr")
            .data(function(object) {
                return object.fields;
            })
            .call(fillVariableRow);

        fields.enter()
            .append("tr")
            .call(newVariableRow);

        fields.exit()
            .remove();
    }

    function newVariableRow(tr) {
        tr.append("td").attr("class", "type");
        tr.append("td").attr("class", "name");
        tr.append("td").attr("class", "value");
        fillVariableRow(tr);
    }

    function fillVariableRow(tr) {
        tr.select(".type").text(function(variable) {
            return variable.type;
        });
        tr.select(".name").text(function(variable) {
            return variable.name;
        });
        tr.select(".value").each(function(variable) {
            if (variable.isReference) {
                // This is an object reference...
                if (variable.value != "null") {
                    deferredPointers.push({"start": this, "target": variable.value})
                } else {
                    d3.select(this).text(variable.value);
                }
            } else {
                // ...this is a primative value.
                d3.select(this).text(variable.value);
            }
        });
    }

    function drawLinks() {
        deferredPointers.forEach(function(pointer) {
            jsplumb.connect({
                source: pointer.start,
                target: "heap-object-" + pointer.target
            });
        });
    }
});
