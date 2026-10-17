package com.xsoftwareforge.xconsoleui.list;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.BrowseState;
import com.xsoftwareforge.xconsoleui.menu.CellBrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.TableCellResult;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableBuilder;

/**
 * A selectable, typed table: like {@link RowBrowser} but rows are laid out into
 * auto-sized columns with a pinned header. Cursor selection, type-ahead filtering
 * (across the rendered row text), and per-row function-key actions.
 * <p>
 * {@link #multiSelect(boolean)} and {@link #batchAction} work as in
 * {@link RowBrowser} (a {@code [x]} column is prepended). Marking is available in
 * row mode only: in cell mode (editable columns / {@link #cellSelectable}) batch
 * actions receive just the cursor row.
 *
 * @param <T> the row element type
 */
public final class TableBrowser<T> {

    private final String title;
    private final Supplier<List<T>> source;
    private final List<TableColumnDef<T>> columns = new ArrayList<>();
    private final Map<FunctionKey, RowAction<T>> actions = new LinkedHashMap<>();
    private boolean filterable = true;
    private boolean cellSelectable;
    private boolean multiSelect;
    private RowAction.Handler<T> onSelect;

    private TableBrowser(String title, Supplier<List<T>> source) {
        this.title = Objects.requireNonNull(title, "title");
        this.source = Objects.requireNonNull(source, "source");
    }

    public static <T> TableBrowser<T> create(String title, List<T> items) {
        List<T> snapshot = List.copyOf(items);
        return new TableBrowser<>(title, () -> snapshot);
    }

    public static <T> TableBrowser<T> create(String title, Supplier<List<T>> source) {
        return new TableBrowser<>(title, source);
    }

    public TableBrowser<T> column(TableColumnDef<T> column) {
        columns.add(Objects.requireNonNull(column, "column"));
        return this;
    }

    public TableBrowser<T> filterable(boolean filterable) {
        this.filterable = filterable;
        return this;
    }

    /**
     * Enable cell-selection mode (Tab toggles row/cell, Left/Right pick a cell,
     * Enter edits an editable cell). Auto-enabled when any column is
     * {@link TableColumnDef#editable editable}.
     */
    public TableBrowser<T> cellSelectable(boolean cellSelectable) {
        this.cellSelectable = cellSelectable;
        return this;
    }

    public TableBrowser<T> action(FunctionKey key, String label, RowAction.Handler<T> handler) {
        actions.put(key, new RowAction<>(key, label, handler));
        return this;
    }

    /** See {@link RowBrowser#multiSelect(boolean)}; row mode only. @since 1.1.0 */
    public TableBrowser<T> multiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
        return this;
    }

    /** See {@link RowBrowser#batchAction}. @since 1.1.0 */
    public TableBrowser<T> batchAction(FunctionKey key, String label, RowAction.BatchHandler<T> handler) {
        actions.put(key, RowAction.batch(key, label, handler));
        return this;
    }

    public TableBrowser<T> onSelect(RowAction.Handler<T> handler) {
        this.onSelect = handler;
        return this;
    }

    public void open(MenuContext context) throws Exception {
        if (cellEnabled()) {
            openCells(context);
            return;
        }
        Map<FunctionKey, String> hints = hints();
        BrowseState state = multiSelect ? new BrowseState() : null;
        Marks<T> marks = new Marks<>();
        while (true) {
            List<T> items = source.get();
            BrowseResult result;
            if (state == null) {
                result = context.browseTable(title, buildTable(items), hints, filterable);
            } else {
                marks.toState(items, state);
                result = context.browseTable(title, buildTable(items), hints, filterable, state);
                marks.fromState(items, state);
            }
            switch (result.kind()) {
            case BACK -> {
                return;
            }
            case SELECT -> {
                if (onSelect == null) {
                    return;
                }
                if (inRange(items, result.index())) {
                    T item = items.get(result.index());
                    RowAction.runReported(context, title, rowKey(item), null, "Select",
                            () -> onSelect.run(context, item));
                }
            }
            case ACTION -> {
                RowAction<T> action = actions.get(result.actionKey());
                if (action != null && inRange(items, result.index())) {
                    T item = items.get(result.index());
                    if (action.isBatch()) {
                        if (RowAction.runBatchReported(context, title, marks.targets(items, item), this::rowKey,
                                action)) {
                            marks.clear();
                        }
                    } else {
                        RowAction.runReported(context, title, rowKey(item), action.key(), action.label(),
                                () -> action.handler().run(context, item));
                    }
                }
            }
            }
        }
    }

    private void openCells(MenuContext context) throws Exception {
        Map<FunctionKey, String> hints = hints();
        CellBrowseState state = new CellBrowseState();
        while (true) {
            List<T> items = source.get();
            TableCellResult result = context.browseTableCells(title, buildTable(items), hints, filterable, state);
            switch (result.kind()) {
            case BACK -> {
                return;
            }
            case ACTION -> {
                RowAction<T> action = actions.get(result.actionKey());
                if (action != null && inRange(items, result.row())) {
                    T item = items.get(result.row());
                    RowAction.runReported(context, title, rowKey(item), action.key(), action.label(),
                            () -> action.handler().run(context, item));
                }
            }
            case SELECT -> {
                if (!inRange(items, result.row())) {
                    continue;
                }
                T item = items.get(result.row());
                if (result.isCell() && result.col() < columns.size() && columns.get(result.col()).isEditable()) {
                    TableColumnDef<T> column = columns.get(result.col());
                    RowAction.runReported(context, title, rowKey(item), null, "Edit " + column.name(),
                            () -> column.editor().edit(context, item));
                } else if (onSelect != null) {
                    RowAction.runReported(context, title, rowKey(item), null, "Select",
                            () -> onSelect.run(context, item));
                } else if (!result.isCell()) {
                    return; // row selected, nothing to do
                }
            }
            }
        }
    }

    private boolean cellEnabled() {
        if (cellSelectable) {
            return true;
        }
        for (TableColumnDef<T> column : columns) {
            if (column.isEditable()) {
                return true;
            }
        }
        return false;
    }

    public Optional<T> pick(MenuContext context) throws Exception {
        List<T> items = source.get();
        BrowseResult result = context.browseTable(title, buildTable(items), Map.of(), filterable);
        if (result.kind() == BrowseResult.Kind.SELECT && inRange(items, result.index())) {
            return Optional.of(items.get(result.index()));
        }
        return Optional.empty();
    }

    private Table buildTable(List<T> items) {
        TableBuilder builder = TableBuilder.create();
        for (TableColumnDef<T> column : columns) {
            builder.column(column.name(), column.align());
        }
        for (T item : items) {
            List<String> cells = new ArrayList<>(columns.size());
            for (TableColumnDef<T> column : columns) {
                cells.add(column.cell(item));
            }
            builder.row(cells);
        }
        return builder.build();
    }

    /** The row's first cell, used as its key when reporting row actions. */
    private String rowKey(T item) {
        return columns.isEmpty() ? "" : columns.get(0).cell(item);
    }

    private Map<FunctionKey, String> hints() {
        Map<FunctionKey, String> hints = new LinkedHashMap<>();
        for (RowAction<T> action : actions.values()) {
            hints.put(action.key(), action.label());
        }
        return hints;
    }

    private static boolean inRange(List<?> items, int index) {
        return index >= 0 && index < items.size();
    }
}
