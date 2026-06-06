package com.xsoftwareforge.xconsoleui.list;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
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
        while (true) {
            List<T> items = source.get();
            BrowseResult result = context.browseTable(title, buildTable(items), hints, filterable);
            switch (result.kind()) {
            case BACK -> {
                return;
            }
            case SELECT -> {
                if (onSelect == null) {
                    return;
                }
                if (inRange(items, result.index())) {
                    onSelect.run(context, items.get(result.index()));
                }
            }
            case ACTION -> {
                RowAction<T> action = actions.get(result.actionKey());
                if (action != null && inRange(items, result.index())) {
                    action.handler().run(context, items.get(result.index()));
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
                    action.handler().run(context, items.get(result.row()));
                }
            }
            case SELECT -> {
                if (!inRange(items, result.row())) {
                    continue;
                }
                T item = items.get(result.row());
                if (result.isCell() && result.col() < columns.size() && columns.get(result.col()).isEditable()) {
                    columns.get(result.col()).editor().edit(context, item);
                } else if (onSelect != null) {
                    onSelect.run(context, item);
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
