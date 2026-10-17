package com.xsoftwareforge.xconsoleui.form;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Deque;
import java.util.List;

import java.util.Map;

import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.BrowseState;
import com.xsoftwareforge.xconsoleui.menu.RowActionEvent;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.TableCellResult;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableExporter;

/**
 * A scripted {@link MenuContext} for testing form logic without a terminal.
 * Queue the values that {@code select}/{@code input}/etc. should return.
 */
public final class StubMenuContext implements MenuContext {

    final Deque<String> selectResults = new ArrayDeque<>();
    final Deque<String> inputResults = new ArrayDeque<>();
    final Deque<List<String>> multiSelectResults = new ArrayDeque<>();
    final Deque<Boolean> confirmResults = new ArrayDeque<>();
    final Deque<BrowseResult> browseResults = new ArrayDeque<>();
    final Deque<TableCellResult> cellResults = new ArrayDeque<>();
    final Deque<Set<Integer>> markResults = new ArrayDeque<>();
    /** Marks each stateful browse call found on entry. */
    public final List<Set<Integer>> marksSeen = new ArrayList<>();
    public final List<String> confirmTitles = new ArrayList<>();
    public final List<String> resultTitles = new ArrayList<>();
    public final List<List<String>> resultLines = new ArrayList<>();
    public final List<RowActionEvent> rowEvents = new ArrayList<>();

    /** Marks the next stateful (multi-select) browse call leaves in its state, as if the user marked them. */
    public StubMenuContext queueMarked(Integer... indices) {
        markResults.addLast(new TreeSet<>(List.of(indices)));
        return this;
    }

    public StubMenuContext queueConfirm(Boolean... values) {
        for (Boolean value : values) {
            confirmResults.addLast(value);
        }
        return this;
    }

    @Override
    public BrowseResult browse(String title, List<String> rows, Map<FunctionKey, String> rowActions,
            boolean filterable, BrowseState state) {
        applyMarks(state);
        return browse(title, rows, rowActions, filterable);
    }

    @Override
    public BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable, BrowseState state) {
        applyMarks(state);
        return browseTable(title, table, rowActions, filterable);
    }

    private void applyMarks(BrowseState state) {
        marksSeen.add(new TreeSet<>(state.marked()));
        if (!markResults.isEmpty()) {
            state.marked().clear();
            state.marked().addAll(markResults.pollFirst());
        }
    }

    @Override
    public void rowActionCompleted(RowActionEvent event) {
        rowEvents.add(event);
    }

    public StubMenuContext queueCell(TableCellResult... results) {
        for (TableCellResult result : results) {
            cellResults.addLast(result);
        }
        return this;
    }

    public StubMenuContext queueBrowse(BrowseResult... results) {
        for (BrowseResult result : results) {
            browseResults.addLast(result);
        }
        return this;
    }

    public StubMenuContext queueSelect(String... ids) {
        for (String id : ids) {
            selectResults.addLast(id);
        }
        return this;
    }

    public StubMenuContext queueInput(String... values) {
        for (String value : values) {
            inputResults.addLast(value);
        }
        return this;
    }

    @Override
    public String select(String title, List<MenuItem> options) {
        return selectResults.isEmpty() ? null : selectResults.pollFirst();
    }

    @Override
    public List<String> selectMultiple(String title, List<MenuItem> options, List<String> preselected) {
        return multiSelectResults.isEmpty() ? null : multiSelectResults.pollFirst();
    }

    @Override
    public String input(String title) {
        return inputResults.isEmpty() ? null : inputResults.pollFirst();
    }

    @Override
    public String input(String title, String defaultValue) {
        return inputResults.isEmpty() ? null : inputResults.pollFirst();
    }

    @Override
    public String inputMasked(String title) {
        return inputResults.isEmpty() ? null : inputResults.pollFirst();
    }

    @Override
    public boolean confirm(String title) {
        confirmTitles.add(title);
        return !confirmResults.isEmpty() && confirmResults.pollFirst();
    }

    @Override
    public boolean confirm(String title, boolean defaultValue) {
        confirmTitles.add(title);
        return confirmResults.isEmpty() ? defaultValue : confirmResults.pollFirst();
    }

    @Override
    public void print(String message) {
    }

    @Override
    public void printLines(List<String> lines) {
    }

    @Override
    public void printSeparator() {
    }

    @Override
    public void printError(String message) {
    }

    @Override
    public void printSuccess(String message) {
    }

    @Override
    public void showResult(String title, List<String> lines) {
        resultTitles.add(title);
        resultLines.add(lines);
    }

    @Override
    public void notify(String title, List<String> lines) {
    }

    @Override
    public void showConsole() {
    }

    @Override
    public BrowseResult browse(String title, List<String> rows, Map<FunctionKey, String> rowActions,
            boolean filterable) {
        return browseResults.isEmpty() ? BrowseResult.back() : browseResults.pollFirst();
    }

    @Override
    public BrowseResult browseTable(String title, com.xsoftwareforge.xconsoleui.table.Table table,
            Map<FunctionKey, String> rowActions, boolean filterable) {
        return browseResults.isEmpty() ? BrowseResult.back() : browseResults.pollFirst();
    }

    @Override
    public TableCellResult browseTableCells(String title, com.xsoftwareforge.xconsoleui.table.Table table,
            Map<FunctionKey, String> rowActions, boolean filterable,
            com.xsoftwareforge.xconsoleui.menu.CellBrowseState state) {
        return cellResults.isEmpty() ? TableCellResult.back() : cellResults.pollFirst();
    }

    @Override
    public void showTable(String title, Table table) {
    }

    @Override
    public void showTable(String title, Table table, TableExporter exporter) {
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void close() {
    }
}
