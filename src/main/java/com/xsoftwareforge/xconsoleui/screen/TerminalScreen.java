package com.xsoftwareforge.xconsoleui.screen;

import java.util.List;
import java.util.Map;

import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.CellBrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.TableCellResult;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableViewAction;

/**
 * Low-level interactive screen primitives, decoupled from any concrete terminal
 * backend. The {@link com.xsoftwareforge.xconsoleui.runner.MenuSession} drives navigation
 * through this interface; tests can supply a scripted implementation.
 */
public interface TerminalScreen {

    /**
     * Render a single-choice list and return the selected item id, or
     * {@code null} if cancelled (Esc).
     */
    String select(String title, List<MenuItem> options) throws Exception;

    /**
     * Render a single-choice list that also reacts to function-key hotkeys.
     *
     * @param hotkeys ordered map of active function keys to their hint labels;
     *                pressing one returns a {@link ScreenChoice#hotkey} result
     * @return the user's choice: an item, a hotkey, or back (Esc/q)
     */
    ScreenChoice select(String title, List<MenuItem> options, Map<FunctionKey, String> hotkeys, boolean grid)
            throws Exception;

    /**
     * Render a multi-choice list and return the selected ids in declaration
     * order, or {@code null} if cancelled (Esc).
     */
    List<String> selectMultiple(String title, List<MenuItem> options, List<String> selectedIds) throws Exception;

    /**
     * Prompt for free text. Returns {@code defaultValue} when the user confirms
     * empty input and a default is set, or {@code null} if cancelled (Esc).
     *
     * @param mask when non-null, the character used to mask typed input
     */
    String input(String title, String defaultValue, Character mask) throws Exception;

    /** Yes/No confirmation with the given default selection. */
    boolean confirm(String title, boolean defaultValue) throws Exception;

    /**
     * Show a scrollable, read-only result screen. When {@code grid} is true the
     * entries are arranged into multiple columns filling the width; the view-toggle
     * key flips the layout live.
     */
    void showResult(String title, List<String> lines, boolean grid) throws Exception;

    /**
     * Show a scrollable table. With {@code stickyHeader} the header stays pinned
     * while rows scroll; {@code h} toggles header visibility. When
     * {@code exportEnabled}, {@code e} returns {@link TableViewAction#EXPORT}.
     */
    TableViewAction showTable(String title, Table table, boolean stickyHeader, boolean exportEnabled) throws Exception;

    /**
     * Interactive row browser: navigate rows with the cursor, optionally filter by
     * typing (type-ahead), select with Enter, or trigger a row hotkey action.
     *
     * @param header      fixed lines pinned above the rows (e.g. a table header), or null
     * @param rows        the display rows
     * @param rowActions  function keys to row-action labels shown as hints
     * @param filterable  whether typing filters the rows
     */
    BrowseResult browse(String title, List<String> header, List<String> rows, Map<FunctionKey, String> rowActions,
            boolean filterable) throws Exception;

    /**
     * Like {@link #browse} but the rows are a {@link Table} re-rendered to the
     * terminal width each redraw, with the header/separator pinned. Type-ahead
     * filters the data rows; the result index refers to the table's row order.
     */
    BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions, boolean filterable)
            throws Exception;

    /**
     * Like {@link #browseTable} but supports a cell-selection mode: Tab toggles
     * between row mode (whole-row highlight) and cell mode (Left/Right pick a cell,
     * single-cell highlight). Returns the selected row or cell, a row action, or
     * back.
     */
    TableCellResult browseTableCells(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable, CellBrowseState state) throws Exception;
}
