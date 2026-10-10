package com.xsoftwareforge.xconsoleui.menu;

import java.util.List;
import java.util.Map;

import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableExporter;

/**
 * Runtime facade handed to a {@link MenuAction} while the action is executing.
 * <p>
 * It exposes buffered output as well as the interactive primitives of the
 * underlying screen (selection lists, free text input, confirmation, scrollable
 * result views). Output produced through this context is coordinated with the
 * active menu so that log lines do not corrupt the rendered screen.
 */
public interface MenuContext {

    /** Print a single line of buffered output. */
    void print(String message);

    /** Print multiple lines of buffered output. */
    void printLines(List<String> lines);

    /** Print a horizontal separator line. */
    void printSeparator();

    /** Print a line prefixed as an error. */
    void printError(String message);

    /** Print a line prefixed as a success message. */
    void printSuccess(String message);

    /** Prompt for free text without a default value. */
    String input(String title) throws Exception;

    /** Prompt for free text, returning {@code defaultValue} on empty input. */
    String input(String title, String defaultValue) throws Exception;

    /** Prompt for masked free text (e.g. a password). */
    String inputMasked(String title) throws Exception;

    /** Yes/No confirmation defaulting to {@code false}. */
    boolean confirm(String title) throws Exception;

    /** Yes/No confirmation with the given default selection. */
    boolean confirm(String title, boolean defaultValue) throws Exception;

    /**
     * Render a single-choice selection list and return the chosen item id, or
     * {@code null} when the user cancelled (Esc).
     */
    String select(String title, List<MenuItem> options) throws Exception;

    /**
     * Render a multi-choice selection list and return the chosen item ids in
     * declaration order, or {@code null} when the user cancelled (Esc).
     */
    List<String> selectMultiple(String title, List<MenuItem> options, List<String> preselected) throws Exception;

    /** Show a scrollable, read-only result screen. */
    void showResult(String title, List<String> lines) throws Exception;

    /**
     * Show a short, always-fullscreen confirmation that the user dismisses with
     * Enter/Esc. Unlike {@link #showResult}, it ignores the list view mode, so it
     * is reliably visible even when triggered from a menu (e.g. a settings toggle
     * hotkey).
     */
    void notify(String title, List<String> lines) throws Exception;

    /**
     * Show the buffered console output (scrollback) in a scrollable view. Useful
     * to read log lines that were buffered while a menu was on screen.
     */
    void showConsole() throws Exception;

    /** Show a scrollable table (auto-sized columns, sticky header per settings). */
    void showTable(String title, Table table) throws Exception;

    /**
     * Show a scrollable table with export enabled. Pressing {@code e} prompts for
     * format (CSV/JSON/both) and a file name, then writes via {@code exporter}.
     */
    void showTable(String title, Table table, TableExporter exporter) throws Exception;

    /**
     * Interactive row browser: cursor navigation, optional type-ahead filtering,
     * Enter to select, and function-key row actions. Typically used through the
     * higher-level {@code RowBrowser}.
     */
    BrowseResult browse(String title, List<String> rows, Map<FunctionKey, String> rowActions, boolean filterable)
            throws Exception;

    /**
     * Interactive, selectable table: cursor selection over rows, type-ahead
     * filtering, pinned header, and function-key row actions. Typically used
     * through the higher-level {@code TableBrowser}.
     */
    BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions, boolean filterable)
            throws Exception;

    /**
     * Cell-capable selectable table: Tab toggles row/cell mode, Left/Right pick a
     * cell. Typically used through the higher-level {@code TableBrowser}.
     */
    TableCellResult browseTableCells(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable, CellBrowseState state) throws Exception;

    /**
     * Called by {@code RowBrowser}/{@code TableBrowser} each time a row action,
     * Enter handler or cell edit returns, so the session can report it while the
     * browser stays open. The default does nothing.
     */
    default void rowActionCompleted(RowActionEvent event) {
    }

    /** @return whether the owning menu session is still running. */
    boolean isRunning();

    /**
     * Close the entire menu session, unwinding to the host (e.g. back to the
     * command prompt). Used by actions such as the built-in "Close menu" hotkey.
     */
    void close();
}
