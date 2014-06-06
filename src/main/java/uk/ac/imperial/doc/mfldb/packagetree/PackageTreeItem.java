package uk.ac.imperial.doc.mfldb.packagetree;

import uk.ac.imperial.doc.mfldb.ui.HierarchyData;

/**
 * Created by graham on 10/05/14.
 */
public interface PackageTreeItem extends HierarchyData<PackageTreeItem> {
    PackageTreeItem lookupChild(String name);
    String getName();
    String getQualifiedName();
}
