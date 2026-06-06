package com.xsoftwareforge.xconsoleui.table;

import java.util.List;

/**
 * Serializes a {@link Table} to CSV (RFC 4180-ish) or a compact JSON array of
 * objects keyed by column name. Dependency-free.
 */
public final class TableSerializer {

    private TableSerializer() {
    }

    public static String toCsv(Table table) {
        StringBuilder out = new StringBuilder();
        List<TableColumn> columns = table.columns();
        for (int c = 0; c < columns.size(); c++) {
            if (c > 0) {
                out.append(',');
            }
            out.append(csvField(columns.get(c).name()));
        }
        out.append('\n');
        for (List<String> row : table.rows()) {
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) {
                    out.append(',');
                }
                out.append(csvField(cell(row, c)));
            }
            out.append('\n');
        }
        return out.toString();
    }

    public static String toJson(Table table) {
        List<TableColumn> columns = table.columns();
        StringBuilder out = new StringBuilder();
        out.append("[\n");
        List<List<String>> rows = table.rows();
        for (int r = 0; r < rows.size(); r++) {
            out.append("  {");
            for (int c = 0; c < columns.size(); c++) {
                if (c > 0) {
                    out.append(", ");
                }
                out.append(jsonString(columns.get(c).name()))
                        .append(": ")
                        .append(jsonString(cell(rows.get(r), c)));
            }
            out.append('}');
            out.append(r < rows.size() - 1 ? ",\n" : "\n");
        }
        out.append("]\n");
        return out.toString();
    }

    private static String cell(List<String> row, int index) {
        if (row == null || index >= row.size() || row.get(index) == null) {
            return "";
        }
        return row.get(index);
    }

    private static String csvField(String value) {
        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
            case '"' -> out.append("\\\"");
            case '\\' -> out.append("\\\\");
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\t' -> out.append("\\t");
            default -> {
                if (ch < 0x20) {
                    out.append(String.format("\\u%04x", (int) ch));
                } else {
                    out.append(ch);
                }
            }
            }
        }
        return out.append('"').toString();
    }
}
