package com.xsoftwareforge.xconsoleui.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for a {@link Table}:
 *
 * <pre>{@code
 * Table t = TableBuilder.create()
 *     .column("Planet")
 *     .column("Pop", Align.RIGHT)
 *     .row("Earth", 8_000)
 *     .row("Mars", 120)
 *     .build();
 * }</pre>
 */
public final class TableBuilder {

    private final List<TableColumn> columns = new ArrayList<>();
    private final List<List<String>> rows = new ArrayList<>();

    public static TableBuilder create() {
        return new TableBuilder();
    }

    public TableBuilder column(String name) {
        columns.add(new TableColumn(name));
        return this;
    }

    public TableBuilder column(String name, Align align) {
        columns.add(new TableColumn(name, align));
        return this;
    }

    /** Add a row; values are converted to strings ({@code null} becomes empty). */
    public TableBuilder row(Object... values) {
        List<String> cells = new ArrayList<>(values.length);
        for (Object value : values) {
            cells.add(value == null ? "" : String.valueOf(value));
        }
        rows.add(cells);
        return this;
    }

    public TableBuilder row(List<String> values) {
        Objects.requireNonNull(values, "values");
        rows.add(new ArrayList<>(values));
        return this;
    }

    public Table build() {
        return new Table(columns, rows);
    }
}
