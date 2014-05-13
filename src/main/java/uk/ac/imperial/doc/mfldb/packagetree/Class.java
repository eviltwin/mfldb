package uk.ac.imperial.doc.mfldb.packagetree;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;

/**
* Created by graham on 11/05/14.
*/
public class Class implements PackageTreeItem {

    private final ReadOnlyStringWrapper name = new ReadOnlyStringWrapper(this, "name");
    private final ObjectProperty<Path> classFilePath = new SimpleObjectProperty<>(this, "classFilePath");
    private final ObjectProperty<Path> javaFilePath = new SimpleObjectProperty<>(this, "javaFilePath");

    public Class(String name) {
        this.name.set(name);
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
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
    }

    public ObjectProperty<Path> javaFilePathProperty() {
        return javaFilePath;
    }

    @Override
    public ObservableList<PackageTreeItem> getChildren() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public String toString() {
        return getName();
    }
}
