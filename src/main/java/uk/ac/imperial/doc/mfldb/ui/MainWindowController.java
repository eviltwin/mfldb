package uk.ac.imperial.doc.mfldb.ui;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import uk.ac.imperial.doc.mfldb.bridge.BreakpointSpec;
import uk.ac.imperial.doc.mfldb.bridge.DebugSession;
import uk.ac.imperial.doc.mfldb.bridge.DebugSessionException;
import uk.ac.imperial.doc.mfldb.packagetree.BreakpointType;
import uk.ac.imperial.doc.mfldb.packagetree.Class;
import uk.ac.imperial.doc.mfldb.packagetree.Package;
import uk.ac.imperial.doc.mfldb.packagetree.PackageTreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static uk.ac.imperial.doc.mfldb.ui.Const.*;

public class MainWindowController {

    private final Map<BreakpointSpec, BreakpointStatus> breakpoints = new LinkedHashMap<>();

    @FXML
    protected Button runButton;

    @FXML
    protected Button suspendButton;

    @FXML
    protected Button stopButton;

    @FXML
    protected Button stepOverButton;

    @FXML
    protected Button stepIntoButton;

    @FXML
    protected Button stepOutButton;

    @FXML
    protected TreeViewWithItems<PackageTreeItem> packageTree;

    @FXML
    protected WebView codeArea;

    @FXML
    protected WebView stackAndHeap;

    private CodeAreaController codeAreaController;
    private StackAndHeapController stackAndHeapController;
    private Class selectedClass;
    private DebugSession session;
    private Package rootPackage;
    private String cmd;

    // Declare this as a lambda because removeListener() doesn't work with method references. Fucking JVM.
    private ChangeListener<DebugSession.State> debugSessionStateChanged = (observable, oldValue, newValue) -> {
        if (newValue == DebugSession.State.RUNNING && oldValue == DebugSession.State.READY) {
            runButton.setText(RERUN_BUTTON_LABEL);
            runButton.setGraphic(new ImageView(RERUN_IMAGE));
            suspendButton.setText(SUSPEND_BUTTON_LABEL);
            suspendButton.setGraphic(new ImageView(SUSPEND_IMAGE));
            suspendButton.setDisable(false);
            stopButton.setDisable(false);
        } else if (newValue == DebugSession.State.SUSPENDED) {
            suspendButton.setText(RESUME_BUTTON_LABEL);
            suspendButton.setGraphic(new ImageView(RESUME_IMAGE));
            stepOverButton.setDisable(false);
            stepIntoButton.setDisable(false);
            stepOutButton.setDisable(false);
            moveCarretToCurrentPosition();
        } else if (newValue == DebugSession.State.RUNNING && oldValue == DebugSession.State.SUSPENDED) {
            suspendButton.setText(SUSPEND_BUTTON_LABEL);
            suspendButton.setGraphic(new ImageView(SUSPEND_IMAGE));
            stepOverButton.setDisable(true);
            stepIntoButton.setDisable(true);
            stepOutButton.setDisable(true);
        } else if (newValue == DebugSession.State.TERMINATED) {
            runButton.setText(RUN_BUTTON_LABEL);
            runButton.setGraphic(new ImageView(RUN_IMAGE));
            suspendButton.setText(SUSPEND_BUTTON_LABEL);
            suspendButton.setGraphic(new ImageView(SUSPEND_IMAGE));
            suspendButton.setDisable(true);
            stopButton.setDisable(true);
            stepOverButton.setDisable(true);
            stepIntoButton.setDisable(true);
            stepOutButton.setDisable(true);
            ensureEnded();
        }
    };

    public void ensureEnded() {
        if (session != null) {
            // Remove the session state changed event handler to avoid handling stale queued events
            session.stateProperty().removeListener(debugSessionStateChanged);
            session.setBreakpointResolutionSuccessCallback(null);
            session.setBreakpointResolutionFailureCallback(null);
            session.ensureEnded();
            session = null;

            // Change all breakpoints to be back to the "ADDED" state.
            breakpoints.replaceAll((spec, status) -> BreakpointStatus.ADDED);
            if (selectedClass != null) {
                refreshBreakpointMarkers();
            }
        }
    }

    public void setCmd(String cmd) throws DebugSessionException, IOException {
        this.cmd = cmd;
    }

    @FXML
    protected void initialize() {
        try {
            rootPackage = Package.buildPackageTree(DEFAULT_PACKAGE_LABEL, Paths.get("."));
        } catch (IOException e) {
            e.printStackTrace();
        }
        packageTree.setTreeItemFactory(this::treeItemFactory);
        packageTree.getSelectionModel().selectedItemProperty().addListener(this::packageTreeSelectionChanged);
        packageTree.setRoot(treeItemFactory(rootPackage));
        codeAreaController = new CodeAreaController(codeArea);
        codeAreaController.setBreakpointToggleHandler(this::handleBreakpointToggle);
        stackAndHeapController = new StackAndHeapController(stackAndHeap);
    }

    @FXML
    protected void onEnd(ActionEvent actionEvent) {
        session.ensureEnded();
    }

    @FXML
    protected void onRun(ActionEvent actionEvent) {
        ensureEnded();
        try {
            session = new DebugSession(cmd);
            session.stateProperty().addListener(debugSessionStateChanged);
            session.setBreakpointResolutionSuccessCallback(this::handleBreakpointResolutionSuccess);
            session.setBreakpointResolutionFailureCallback(this::handleBreakpointResolutionFailure);
            breakpoints.keySet().forEach(session::addBreakpoint);
            session.resume();
        } catch (DebugSessionException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onSuspend(ActionEvent actionEvent) {
        if (session.getState() == DebugSession.State.RUNNING) {
            session.pause();
        } else if (session.getState() == DebugSession.State.SUSPENDED) {
            session.resume();
        }
    }

    @FXML
    protected void onStepOver(ActionEvent actionEvent) {
        session.stepOver(session.getCurrentThread());
    }

    @FXML
    protected void onStepInto(ActionEvent actionEvent) {
        session.stepInto(session.getCurrentThread());
    }

    @FXML
    protected void onStepOut(ActionEvent actionEvent) {
        session.stepOut(session.getCurrentThread());
    }

    private TreeItem<PackageTreeItem> treeItemFactory(PackageTreeItem item) {
        if (item instanceof Package) {
            return new TreeItem<>(item, new ImageView(PACKAGE_IMAGE));
        } else if (item instanceof Class) {
            return new TreeItem<>(item, new ImageView(CLASS_IMAGE));
        } else {
            return new TreeItem<>(item);
        }
    }

    private void packageTreeSelectionChanged(ObservableValue<? extends TreeItem<PackageTreeItem>> observable, TreeItem<PackageTreeItem> oldValue, TreeItem<PackageTreeItem> newValue) {
        PackageTreeItem item = newValue.getValue();
        if (item instanceof Class) {
            openFile((Class) item);
        }
    }

    private void openFile(Class item) {
        Path javaFile = item.getJavaFilePath();
        if (javaFile != null && Files.isReadable(javaFile)) {
            try {
                codeAreaController.replaceText(new String(Files.readAllBytes(javaFile)));
                selectedClass = item;
                refreshBreakpointMarkers();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleBreakpointToggle(int lineNo) {
        BreakpointType candidateType = selectedClass.getBreakpointTypeMap().get(Long.valueOf(lineNo));
        if (candidateType != null) {
            BreakpointSpec spec = new BreakpointSpec(selectedClass.getQualifiedName(), lineNo);
            if (!breakpoints.containsKey(spec)) {
                breakpoints.put(spec, BreakpointStatus.ADDED);
                codeAreaController.markBreakpoint(lineNo, BreakpointType.LINE);
                if (session != null && !session.isTerminated()) {
                    session.addBreakpoint(spec);
                }
            } else {
                breakpoints.remove(spec);
                codeAreaController.clearBreakpoint(lineNo);
                if (session != null && !session.isTerminated()) {
                    session.removeBreakpoint(spec);
                }
            }
        }
    }

    private void handleBreakpointResolutionSuccess(BreakpointSpec spec) {
        breakpoints.put(spec, BreakpointStatus.RESOLVED);
        if (spec.className.equals(selectedClass.getQualifiedName())) {
            codeAreaController.markBreakpointResolved(spec.lineNumber, BreakpointType.LINE);
        }
    }

    private void handleBreakpointResolutionFailure(BreakpointSpec spec, Exception e) {
        breakpoints.put(spec, BreakpointStatus.FAILED);
        if (spec.className.equals(selectedClass.getQualifiedName())) {
            codeAreaController.markBreakpointResolutionFailed(spec.lineNumber, BreakpointType.LINE);
        }
    }

    private void refreshBreakpointMarkers() {
        breakpoints.forEach((spec, status) -> {
            if (selectedClass != null && spec.className.equals(selectedClass.getQualifiedName())) {
                switch (status) {
                    case ADDED:
                        codeAreaController.markBreakpoint(spec.lineNumber, BreakpointType.LINE);
                        break;
                    case RESOLVED:
                        codeAreaController.markBreakpointResolved(spec.lineNumber, BreakpointType.LINE);
                        break;
                    case FAILED:
                        codeAreaController.markBreakpointResolutionFailed(spec.lineNumber, BreakpointType.LINE);
                        break;
                }
            }
        });
    }

    private void moveCarretToCurrentPosition() {
        ThreadReference currentThread = session.getCurrentThread();
        try {
            StackFrame frame = currentThread.frame(0);
            Location location = frame.location();
            Class target = (Class) rootPackage.lookupChild(location.declaringType().name());
            if (target != null) {
                openFile(target);
                codeAreaController.jumpToLine(location.lineNumber());
                codeAreaController.markCurrentLine(location.lineNumber());
                stackAndHeapController.buildViewFor(currentThread.frames());
            }
        } catch (IncompatibleThreadStateException e) {
            //e.printStackTrace();
        }
    }

    private enum BreakpointStatus {ADDED, RESOLVED, FAILED}
}
