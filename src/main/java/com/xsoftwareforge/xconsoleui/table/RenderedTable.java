package com.xsoftwareforge.xconsoleui.table;

import java.util.List;

/**
 * A table laid out to a fixed width: the {@code header} line, a {@code separator}
 * line, and the formatted {@code rows}. All lines share the same display width,
 * so the header can be pinned while the rows scroll.
 */
public record RenderedTable(String header, String separator, List<String> rows) {

    public RenderedTable {
        rows = List.copyOf(rows);
    }

    public int width() {
        return header.length();
    }
}
