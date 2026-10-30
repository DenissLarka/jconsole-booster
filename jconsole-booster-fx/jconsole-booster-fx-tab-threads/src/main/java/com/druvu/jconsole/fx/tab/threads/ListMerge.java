package com.druvu.jconsole.fx.tab.threads;

import java.util.HashSet;
import java.util.List;

/**
 * Brings a shown list to a wanted one in place, so a list view keeps its selection and scroll position across updates.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
final class ListMerge {

    private ListMerge() {}

    /**
     * Removes from {@code shown} the items {@code wanted} no longer has and inserts the new ones where {@code wanted}
     * puts them. The items both lists hold must be in the same order in each, as the thread lists are: by thread id.
     */
    static <T> void merge(List<T> shown, List<T> wanted) {
        shown.retainAll(new HashSet<>(wanted));
        for (int i = 0; i < wanted.size(); i++) {
            T item = wanted.get(i);
            if (i == shown.size() || !shown.get(i).equals(item)) {
                shown.add(i, item);
            }
        }
    }
}
