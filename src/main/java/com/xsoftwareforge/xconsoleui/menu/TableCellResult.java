package com.xsoftwareforge.xconsoleui.menu;

/**
 * Outcome of a cell-capable table browser. In row mode a selection has
 * {@code col == -1}; in cell mode {@code col} is the selected column.
 * {@link #cellMode()} reports the mode that was active, so the caller can re-open
 * the browser in the same mode after handling the result.
 */
public final class TableCellResult {

    public enum Kind {
        SELECT,
        ACTION,
        BACK
    }

    private static final TableCellResult BACK = new TableCellResult(Kind.BACK, -1, -1, null, false);

    private final Kind kind;
    private final int row;
    private final int col;
    private final FunctionKey actionKey;
    private final boolean cellMode;

    private TableCellResult(Kind kind, int row, int col, FunctionKey actionKey, boolean cellMode) {
        this.kind = kind;
        this.row = row;
        this.col = col;
        this.actionKey = actionKey;
        this.cellMode = cellMode;
    }

    public static TableCellResult selectRow(int row) {
        return new TableCellResult(Kind.SELECT, row, -1, null, false);
    }

    public static TableCellResult selectCell(int row, int col) {
        return new TableCellResult(Kind.SELECT, row, col, null, true);
    }

    public static TableCellResult action(FunctionKey key, int row, boolean cellMode) {
        return new TableCellResult(Kind.ACTION, row, -1, key, cellMode);
    }

    public static TableCellResult back() {
        return BACK;
    }

    public Kind kind() {
        return kind;
    }

    public int row() {
        return row;
    }

    /** Selected column in cell mode, or -1 in row mode. */
    public int col() {
        return col;
    }

    public FunctionKey actionKey() {
        return actionKey;
    }

    /** Whether cell mode was active when this result was produced. */
    public boolean cellMode() {
        return cellMode;
    }

    public boolean isCell() {
        return col >= 0;
    }
}
