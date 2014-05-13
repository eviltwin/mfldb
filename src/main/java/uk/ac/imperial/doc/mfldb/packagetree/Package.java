package uk.ac.imperial.doc.mfldb.packagetree;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.text.Collator;
import java.util.HashMap;
import java.util.Map;

import static uk.ac.imperial.doc.mfldb.packagetree.Const.*;

/**
 * Created by graham on 09/05/14.
 */
public class Package implements PackageTreeItem {

    private final ReadOnlyStringWrapper name = new ReadOnlyStringWrapper(this, "name");

    private final ObservableList<PackageTreeItem> realChildren = FXCollections.observableArrayList();
    private final ReadOnlyListWrapper<PackageTreeItem> children = new ReadOnlyListWrapper<>(this, "children", FXCollections.unmodifiableObservableList(realChildren));

    private final Map<String, Class> classes = new HashMap<>();

    public Package(String name, Path root) throws IOException {
        this.name.set(name);

        DirectoryStream.Filter<Path> filter = p -> Files.isDirectory(p)
                || p.toString().toLowerCase().endsWith(JAVA_FILE_EXTENSION)
                || p.toString().toLowerCase().endsWith(CLASS_FILE_EXTENSION);
        try (DirectoryStream<Path> dir = Files.newDirectoryStream(root, filter)) {
            for (Path path : dir) {
                if (Files.isDirectory(path)) {
                    realChildren.add(new Package(path));
                } else {
                    updateOrCreateClassEntry(path);
                }
            }
            realChildren.sort(this::packagesFirst);
        } catch (NotDirectoryException e) {
            throw e;
        } catch (IOException e) {
            throw new IOException("Unknown IOException occurred whilst building PackageTree", e);
        }
    }

    public Package(Path root) throws IOException {
        this(root.getName(root.getNameCount() - 1).toString(), root);
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
    }

    @Override
    public ObservableList<PackageTreeItem> getChildren() {
        return children.get();
    }

    public ReadOnlyListProperty<PackageTreeItem> childrenProperty() {
        return children.getReadOnlyProperty();
    }

    @Override
    public String toString() {
        return getName();
    }

    private void updateOrCreateClassEntry(Path path) {
        String filename = path.getName(path.getNameCount() - 1).toString();
        String classname = filename.substring(0, filename.lastIndexOf('.'));

        Class child = classes.get(classname);
        if (child == null) {
            child = new Class(classname);
            realChildren.add(child);
            classes.put(classname, child);
        }
        if (filename.endsWith(JAVA_FILE_EXTENSION)) {
            child.setJavaFilePath(path);
        } else if (filename.endsWith(CLASS_FILE_EXTENSION)) {
            child.setClassFilePath(path);
        }
    }

    private int packagesFirst(PackageTreeItem source, PackageTreeItem target) {
        Collator collator = Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        if (source instanceof Package && !(target instanceof Package)) {
            return -1;
        } else if (!(source instanceof Package) && target instanceof Package) {
            return 1;
        } else {
            return collator.compare(source.getName(), target.getName());
        }
    }
}
