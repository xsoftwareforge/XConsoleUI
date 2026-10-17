package com.xsoftwareforge.xconsoleui.menu;

import java.util.Set;
import java.util.TreeSet;

/**
 * Mutable state of a multi-select row browser, carried across redraws so that
 * re-opening the browser (e.g. after a row action) restores the cursor row, the
 * type-ahead filter and the marked rows.
 * <p>
 * {@code row} and the {@link #marked()} indices refer to the rows passed to
 * {@code browse} (before filtering), so marks survive filter changes. The browser
 * reads the state on entry and writes cursor and filter back before returning;
 * {@link #marked()} is updated in place.
 */
public final class BrowseState {

    private int row;
    private String filter = "";
    private final Set<Integer> marked = new TreeSet<>();

    public int row() {
        return row;
    }

    public void setRow(int row) {
        this.row = Math.max(0, row);
    }

    public String filter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter;
    }

    /** The live, ordered set of marked row indices. */
    public Set<Integer> marked() {
        return marked;
    }
}
