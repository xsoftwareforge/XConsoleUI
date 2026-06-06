package com.xsoftwareforge.xconsoleui.list;

import java.util.Objects;
import java.util.function.Function;

import com.xsoftwareforge.xconsoleui.table.Align;

/**
 * A typed table column for {@link TableBrowser}: a header name, alignment, and a
 * function mapping a row element to its cell text.
 *
 * @param <T> the row element type
 */
public final class TableColumnDef<T> {

    private final String name;
    private final Align align;
    private final Function<T, String> value;
    private CellEditor<T> editor;

    public TableColumnDef(String name, Align align, Function<T, String> value) {
        this.name = Objects.requireNonNull(name, "name");
        this.align = align == null ? Align.LEFT : align;
        this.value = Objects.requireNonNull(value, "value");
    }

    /** Make this column editable in {@link TableBrowser} cell mode. */
    public TableColumnDef<T> editable(CellEditor<T> editor) {
        this.editor = Objects.requireNonNull(editor, "editor");
        return this;
    }

    public boolean isEditable() {
        return editor != null;
    }

    public CellEditor<T> editor() {
        return editor;
    }

    public static <T> TableColumnDef<T> of(String name, Function<T, String> value) {
        return new TableColumnDef<>(name, Align.LEFT, value);
    }

    public static <T> TableColumnDef<T> of(String name, Align align, Function<T, String> value) {
        return new TableColumnDef<>(name, align, value);
    }

    public String name() {
        return name;
    }

    public Align align() {
        return align;
    }

    public String cell(T item) {
        String text = value.apply(item);
        return text == null ? "" : text;
    }
}
