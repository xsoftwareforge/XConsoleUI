package com.xsoftwareforge.xconsoleui.menu;

/**
 * Mutable cursor state for a cell-capable table browser, carried across redraws
 * so that re-opening the browser (e.g. after editing a cell) restores the same
 * row, column, mode, and filter the user was on.
 * <p>
 * {@code row} is an original row index (before filtering). The browser reads the
 * state on entry and writes the current position back before returning.
 */
public final class CellBrowseState {

    private int row;
    private int col;
    private boolean cellMode;
    private String filter = "";

    public int row() {
        return row;
    }

    public void setRow(int row) {
        this.row = Math.max(0, row);
    }

    public int col() {
        return col;
    }

    public void setCol(int col) {
        this.col = Math.max(0, col);
    }

    public boolean cellMode() {
        return cellMode;
    }

    public void setCellMode(boolean cellMode) {
        this.cellMode = cellMode;
    }

    public String filter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter;
    }
}
