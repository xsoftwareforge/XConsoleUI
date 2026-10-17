package com.xsoftwareforge.xconsoleui.list;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.xsoftwareforge.xconsoleui.menu.BrowseState;

/**
 * Marked rows of a multi-select browser, kept as elements (by {@code equals}) so
 * marks follow their rows when a live source adds or removes entries between
 * redraws. Rows that disappear from the source drop their mark.
 */
final class Marks<T> {

    private final Set<T> items = new LinkedHashSet<>();

    /** Write the marks as row indices of {@code rows} into {@code state}. */
    void toState(List<T> rows, BrowseState state) {
        state.marked().clear();
        for (int i = 0; i < rows.size(); i++) {
            if (items.contains(rows.get(i))) {
                state.marked().add(i);
            }
        }
    }

    /** Read the marks back from the row indices in {@code state}. */
    void fromState(List<T> rows, BrowseState state) {
        items.clear();
        for (int index : state.marked()) {
            if (index >= 0 && index < rows.size()) {
                items.add(rows.get(index));
            }
        }
    }

    /** The marked rows in list order, or just {@code cursor} when nothing is marked. */
    List<T> targets(List<T> rows, T cursor) {
        List<T> targets = new ArrayList<>();
        for (T row : rows) {
            if (items.contains(row)) {
                targets.add(row);
            }
        }
        return targets.isEmpty() ? List.of(cursor) : targets;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    void clear() {
        items.clear();
    }
}
