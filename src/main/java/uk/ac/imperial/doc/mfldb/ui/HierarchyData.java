package uk.ac.imperial.doc.mfldb.ui;

import javafx.collections.ObservableList;

/**
 * Used to mark an object as hierarchical data.
 * This object can then be used as data source for an hierarchical control, like the {@link javafx.scene.control.TreeView}.
 *
 * @author Christian Schudt
 */
public interface HierarchyData<T extends HierarchyData> {
    /**
     * The children collection, which represents the recursive nature of the hierarchy.
     * Each child is again a {@link HierarchyData}.
     *
     * @return A list of children.
     */
    ObservableList<T> getChildren();
}
