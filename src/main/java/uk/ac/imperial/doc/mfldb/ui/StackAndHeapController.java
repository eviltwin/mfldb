package uk.ac.imperial.doc.mfldb.ui;

import com.google.common.collect.Lists;
import com.sun.jdi.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.*;
import java.util.stream.Collectors;

import static uk.ac.imperial.doc.mfldb.ui.Const.STACK_AND_HEAP_HTML;

/**
 * Wraps a {@link WebView} for use with d3.js as a stack and heap diagram.
 */
public class StackAndHeapController {

    private final WebView webView;

    private final Shim shim = new Shim();

    public StackAndHeapController(WebView webView) {
        this.webView = webView;
        WebEngine engine = webView.getEngine();

        // Attach the shim to the WebView's "window" object, making it a global variable in JavaScript.
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("shim", shim);

        // Load the stack and heap view, it will then register itself with the shim.
        engine.load(getClass().getResource(STACK_AND_HEAP_HTML).toExternalForm());
    }

    public void buildViewFor(List<StackFrame> frames) {
        Set<ObjectReference> unresolvedReferences = new LinkedHashSet<>();

        FrameInfo[] stack = Lists.reverse(frames).stream()
                .map(frame -> {
                    String function = frame.location().method().name();
                    VariableInfo[] info = null;
                    try {
                        info = frame.visibleVariables().stream()
                                .map(variable -> infoFromLocalVariable(frame, variable, unresolvedReferences))
                                .toArray(VariableInfo[]::new);
                    } catch (AbsentInformationException e) {
                        //e.printStackTrace();
                    }
                    return new FrameInfo(function, info);
                })
                .toArray(FrameInfo[]::new);

        Set<ObjectReference> processedHeap = new LinkedHashSet<>();

        List<HeapObjectInfo> heap = new ArrayList<>();

        do {
            Set<ObjectReference> current = new LinkedHashSet<>(unresolvedReferences);
            current.removeAll(processedHeap);
            unresolvedReferences.clear();

            heap.addAll(current.stream()
                    .map(reference -> {
                        String id = Long.toString(reference.uniqueID());
                        String type = reference.referenceType().name();
                        VariableInfo[] variables = reference.referenceType().visibleFields().stream()
                                .map(field -> infoFromField(reference, field, unresolvedReferences))
                                .toArray(VariableInfo[]::new);
                        return new HeapObjectInfo(id, type, variables);
                    })
                    .collect(Collectors.toList()));
            processedHeap.addAll(current);
        } while (!processedHeap.containsAll(unresolvedReferences));

        shim.drawStackAndHeap(stack, heap.stream().toArray(HeapObjectInfo[]::new));
    }

    private static VariableInfo infoFromLocalVariable(StackFrame frame, LocalVariable variable, Set<ObjectReference> heapReferences) {
        String type = variable.typeName();
        String name = variable.name();
        return infoFromValue(type, name, frame.getValue(variable), heapReferences);
    }

    private static VariableInfo infoFromField(ObjectReference reference, Field field, Set<ObjectReference> heapReferences) {
        String type = field.typeName();
        String name = field.name();
        return infoFromValue(type, name, reference.getValue(field), heapReferences);
    }

    private static VariableInfo infoFromValue(String type, String name, Value value, Set<ObjectReference> heapReferences) {
        String sValue;
        boolean isReference = false;
        if (value instanceof BooleanValue) {
            sValue = Boolean.toString(((BooleanValue) value).value());
        } else if (value instanceof ByteValue) {
            sValue = Byte.toString(((ByteValue) value).value());
        } else if (value instanceof CharValue) {
            sValue = Character.toString(((CharValue) value).value());
        } else if (value instanceof DoubleValue) {
            sValue = Double.toString(((DoubleValue) value).value());
        } else if (value instanceof FloatValue) {
            sValue = Float.toString(((FloatValue) value).value());
        } else if (value instanceof IntegerValue) {
            sValue = Integer.toString(((IntegerValue) value).value());
        } else if (value instanceof LongValue) {
            sValue = Long.toString(((LongValue) value).value());
        } else if (value instanceof ShortValue) {
            sValue = Short.toString(((ShortValue) value).value());
        } else if (value instanceof VoidValue) {
            sValue = "void";
        } else if (value instanceof StringReference) {
            sValue = ((StringReference) value).value();
        } else if (value instanceof ObjectReference) {
            ObjectReference reference = (ObjectReference) value;
            heapReferences.add(reference);
            sValue = Long.toString(reference.uniqueID());
            isReference = true;
        } else if (value == null) {
            sValue = "null";
        } else {
            sValue = "UNKNOWN";
        }
        return new VariableInfo(type, name, sValue, isReference);
    }

    protected class Shim {
        private JSObject jsShim;

        public void registerShimObject(JSObject jsShim) {
            this.jsShim = jsShim;
        }

        public void drawStackAndHeap(FrameInfo[] frames, HeapObjectInfo[] heap) {
            jsShim.call("drawStackAndHeap", new Object[]{frames, heap});
        }
    }

    protected static class FrameInfo {
        public final String function;
        public final VariableInfo[] variables;

        public FrameInfo(String function, VariableInfo[] variables) {
            this.function = function;
            this.variables = variables;
        }
    }

    protected static class HeapObjectInfo {
        public final String id;
        public final String klass;
        public final VariableInfo[] fields;

        public HeapObjectInfo(String id, String klass, VariableInfo[] fields) {
            this.id = id;
            this.klass = klass;
            this.fields = fields;
        }
    }

    protected static class VariableInfo {
        public final String type;
        public final String name;
        public final String value;
        public final boolean isReference;

        private VariableInfo(String type, String name, String value, boolean isReference) {
            this.type = type;
            this.name = name;
            this.value = value;
            this.isReference = isReference;
        }
    }
}
