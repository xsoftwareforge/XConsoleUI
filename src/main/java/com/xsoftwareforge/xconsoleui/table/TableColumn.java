package com.xsoftwareforge.xconsoleui.table;

import java.util.Objects;

/** A table column: a header name and an alignment for its cells. */
public record TableColumn(String name, Align align) {

    public TableColumn {
        Objects.requireNonNull(name, "name");
        if (align == null) {
            align = Align.LEFT;
        }
    }

    public TableColumn(String name) {
        this(name, Align.LEFT);
    }
}
