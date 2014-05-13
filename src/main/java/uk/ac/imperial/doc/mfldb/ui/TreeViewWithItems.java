package uk.ac.imperial.doc.mfldb.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class extends the {@link TreeView} to use items as a data source.
 * <p>
 * This allows you to treat a {@link TreeView} in a similar way as a {@link javafx.scene.control.ListView} or {@link javafx.scene.control.TableView}.
 * <p>
 * Each item in the list must implement the {@link HierarchyData} interface, in order to map the recursive nature of the tree data to the tree view.
 * <p>
 * Each change in the underlying data (adding, removing, sorting) will then be automatically reflected in the UI.
 *
 * @author Christian Schudt
 */
public class TreeViewWithItems<T extends HierarchyData<T>> extends TreeView<T> {

    /**
     * Keep hard references for each listener, so that they don't get garbage collected too soon.
     */
    private final Map<TreeItem<T>, ListChangeListener<T>> hardReferences = new HashMap<>();

    /**
     * Also store a reference from each tree item to its weak listeners, so that the listener can be removed, when the tree item gets removed.
     */
    private final Map<TreeItem<T>, WeakListChangeListener<T>> weakListeners = new HashMap<>();

    private final ObjectProperty<Function<T, TreeItem<T>>> treeItemFactory = new SimpleObjectProperty<>(this, "treeItemFactory", TreeItem<T>::new);

    public TreeViewWithItems() {
        this(null);
    }

    /**
     * Creates the tree view.
     *
     * @param root The root tree item.
     * @see TreeView#TreeView(javafx.scene.control.TreeItem)
     */
    public TreeViewWithItems(TreeItem<T> root) {
        super(null);

        if (root != null) {
            setRoot(addRecursively(root));
        }

        rootProperty().addListener((observableValue, oldRoot, newRoot) -> {
            removeRecursively(oldRoot);
            addRecursively(newRoot);
        });
    }

    public Function<T, TreeItem<T>> getTreeItemFactory() {
        return treeItemFactory.get();
    }

    public void setTreeItemFactory(Function<T, TreeItem<T>> treeItemFactory) {
        this.treeItemFactory.set(treeItemFactory);
    }

    public ObjectProperty<Function<T, TreeItem<T>>> treeItemFactoryProperty() {
        return treeItemFactory;
    }

    /**
     * Gets a {@link javafx.collections.ListChangeListener} for a  {@link TreeItem}. It listens to changes on the underlying list and updates the UI accordingly.
     *
     * @param treeItemChildren The associated tree item's children list.
     * @return The listener.
     */
    private ListChangeListener<T> getListChangeListener(final ObservableList<TreeItem<T>> treeItemChildren) {
        return change -> {
            while (change.next()) {
                if (change.wasUpdated()) {
                    // http://javafx-jira.kenai.com/browse/RT-23434
                    continue;
                }
                if (change.wasRemoved()) {
                    for (int i = change.getRemovedSize() - 1; i >= 0; i--) {
                        removeRecursively(treeItemChildren.remove(change.getFrom() + i));
                    }
                }
                // If items have been added
                if (change.wasAdded()) {
                    // Get the new items
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        treeItemChildren.add(i, addRecursively(change.getList().get(i)));
                    }
                }
                // If the list was sorted.
                if (change.wasPermutated()) {
                    // Store the new order.
                    Map<Integer, TreeItem<T>> tempMap = new HashMap<>();

                    for (int i = change.getTo() - 1; i >= change.getFrom(); i--) {
                        int a = change.getPermutation(i);
                        tempMap.put(a, treeItemChildren.remove(i));
                    }

                    getSelectionModel().clearSelection();

                    // Add the items in the new order.
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        treeItemChildren.add(tempMap.remove(i));
                    }
                }
            }
        };
    }

    /**
     * Removes the listener recursively.
     *
     * @param item The tree item.
     */
    private TreeItem<T> removeRecursively(TreeItem<T> item) {
        if (item != null && item.getValue() != null && item.getValue().getChildren() != null) {
            if (weakListeners.containsKey(item)) {
                item.getValue().getChildren().removeListener(weakListeners.remove(item));
                hardReferences.remove(item);
            }
            item.getChildren().forEach(this::removeRecursively);
        }
        return item;
    }

    /**
     * Adds the children to the tree recursively.
     *
     * @param value The initial value.
     * @return The tree item.
     */
    private TreeItem<T> addRecursively(T value) {
        return addRecursively(treeItemFactory.get().apply(value));
    }

    private TreeItem<T> addRecursively(TreeItem<T> treeItem) {
        T value = treeItem.getValue();
        if (value != null && value.getChildren() != null) {
            ListChangeListener<T> listChangeListener = getListChangeListener(treeItem.getChildren());
            WeakListChangeListener<T> weakListener = new WeakListChangeListener<>(listChangeListener);
            value.getChildren().addListener(weakListener);

            hardReferences.put(treeItem, listChangeListener);
            weakListeners.put(treeItem, weakListener);
            value.getChildren().forEach(c -> treeItem.getChildren().add(addRecursively(c)));
        }
        return treeItem;
    }
}
