package uk.ac.imperial.doc.mfldb.packagetree;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Created by graham on 11/05/14.
 */
public class Class implements PackageTreeItem {

    private final ReadOnlyStringWrapper name = new ReadOnlyStringWrapper(this, "name");
    private final ReadOnlyStringWrapper qualifiedName = new ReadOnlyStringWrapper(this, "qualifiedName");
    private final ObjectProperty<Path> classFilePath = new SimpleObjectProperty<>(this, "classFilePath");
    private final ObjectProperty<Path> javaFilePath = new SimpleObjectProperty<>(this, "javaFilePath");

    private Map<Long, BreakpointType> breakpointTypeMap = null;

    protected Class(String name, String qualifiedName) {
        this.name.set(name);
        this.qualifiedName.set(qualifiedName);
    }

    @Override
    public PackageTreeItem lookupChild(String name) {
        return getQualifiedName().equals(name) ? this : null;
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
    }

    public String getQualifiedName() {
        return qualifiedName.get();
    }

    public ReadOnlyStringProperty qualifiedNameProperty() {
        return qualifiedName.getReadOnlyProperty();
    }

    public Path getClassFilePath() {
        return classFilePath.get();
    }

    public void setClassFilePath(Path classFilePath) {
        this.classFilePath.set(classFilePath);
    }

    public ObjectProperty<Path> classFilePathProperty() {
        return classFilePath;
    }

    public Path getJavaFilePath() {
        return javaFilePath.get();
    }

    public void setJavaFilePath(Path javaFilePath) {
        this.javaFilePath.set(javaFilePath);
        breakpointTypeMap = null;
    }

    public ObjectProperty<Path> javaFilePathProperty() {
        return javaFilePath;
    }

    public Map<Long, BreakpointType> getBreakpointTypeMap() {
        if (breakpointTypeMap == null && getJavaFilePath() != null) {
            buildBreakpointMap();
        }
        // GIVE ME NULL-COALESCE OR GIVE ME DEATH!
        return breakpointTypeMap == null ? Collections.emptyMap() : breakpointTypeMap;
    }

    @Override
    public ObservableList<PackageTreeItem> getChildren() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public String toString() {
        return getName();
    }

    private void buildBreakpointMap() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(getJavaFilePath().toFile());
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);

        Trees trees = Trees.instance(task);
        BreakpointCandidateScanner scanner = new BreakpointCandidateScanner();

        try {
            breakpointTypeMap = scanner.scan(task.parse(), trees.getSourcePositions());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
