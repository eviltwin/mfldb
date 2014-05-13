package uk.ac.imperial.doc.mfldb.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;
import uk.ac.imperial.doc.mfldb.bridge.DebugSession;
import uk.ac.imperial.doc.mfldb.bridge.DebugSessionException;
import uk.ac.imperial.doc.mfldb.packagetree.Class;
import uk.ac.imperial.doc.mfldb.packagetree.Package;
import uk.ac.imperial.doc.mfldb.packagetree.PackageTreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;

import static uk.ac.imperial.doc.mfldb.ui.Const.*;

public class MainWindowController {

    @FXML
    protected Button runButton;

    @FXML
    protected Button suspendButton;

    @FXML
    protected Button stopButton;

    @FXML
    protected TreeViewWithItems<PackageTreeItem> packageTree;

    @FXML
    protected CodeArea codeArea;

    private DebugSession session;
    private Package rootPackage;
    private String cmd;

    // Declare this as a lambda because removeListener() doesn't work with method references. Fucking JVM.
    private ChangeListener<DebugSession.State> debugSessionStateChanged = (observable, oldValue, newValue) -> {
        if (newValue == DebugSession.State.RUNNING && oldValue == DebugSession.State.READY) {
            runButton.setText("Rerun");
            runButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("rerun@2x.png"))));
            suspendButton.setDisable(false);
            stopButton.setDisable(false);
        } else if (newValue == DebugSession.State.SUSPENDED) {
            suspendButton.setText("Resume");
            suspendButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("resume@2x.png"))));
        } else if (newValue == DebugSession.State.RUNNING && oldValue == DebugSession.State.SUSPENDED) {
            suspendButton.setText("Pause");
            suspendButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("suspend@2x.png"))));
        } else if (newValue == DebugSession.State.TERMINATED) {
            runButton.setText("Run");
            runButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("run@2x.png"))));
            suspendButton.setText("Pause");
            suspendButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("suspend@2x.png"))));
            suspendButton.setDisable(true);
            stopButton.setDisable(true);
        }
    };

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = KEYWORD_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton("keyword"), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public void ensureEnded() {
        if (session != null) {
            // Remove the session state changed event handler to avoid handling stale queued events
            session.stateProperty().removeListener(debugSessionStateChanged);
            session.ensureEnded();
            session = null;
        }
    }

    public void setCmd(String cmd) throws DebugSessionException, IOException {
        this.cmd = cmd;
    }

    @FXML
    protected void initialize() {
        try {
            rootPackage = new Package(DEFAULT_PACKAGE_LABEL, Paths.get("."));
        } catch (IOException e) {
            e.printStackTrace();
        }
        packageTree.setTreeItemFactory(this::treeItemFactory);
        packageTree.getSelectionModel().selectedItemProperty().addListener(this::packageTreeSelectionChanged);
        packageTree.setRoot(treeItemFactory(rootPackage));

        codeArea.textProperty().addListener((obs, oldText, newText) -> codeArea.setStyleSpans(0, computeHighlighting(newText)));
    }

    @FXML
    protected void onEnd(ActionEvent actionEvent) {
        ensureEnded();
    }

    @FXML
    protected void onRun(ActionEvent actionEvent) {
        ensureEnded();
        try {
            session = new DebugSession(cmd);
            session.stateProperty().addListener(debugSessionStateChanged);
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
            Path javaFile = ((Class) item).getJavaFilePath();
            if (javaFile != null && Files.isReadable(javaFile)) {
                try {
                    codeArea.replaceText(new String(Files.readAllBytes(javaFile)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
