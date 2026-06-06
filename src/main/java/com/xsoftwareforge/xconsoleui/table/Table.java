package com.xsoftwareforge.xconsoleui.table;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable table of string cells with named, alignable columns. Build one
 * with {@link TableBuilder}. {@link #render(int)} lays it out to a target width,
 * computing each column's width from its header and content and truncating with
 * an ellipsis only when the whole table would exceed the available width.
 */
public final class Table {

    private static final String COLUMN_SEP = " | ";
    private static final String SEP_JOINT = "-+-";
    private static final String ELLIPSIS = "…";

    private final List<TableColumn> columns;
    private final List<List<String>> rows;

    Table(List<TableColumn> columns, List<List<String>> rows) {
        this.columns = List.copyOf(columns);
        List<List<String>> copy = new ArrayList<>();
        for (List<String> row : rows) {
            copy.add(List.copyOf(row));
        }
        this.rows = List.copyOf(copy);
    }

    public List<TableColumn> columns() {
        return columns;
    }

    public List<List<String>> rows() {
        return rows;
    }

    public int columnCount() {
        return columns.size();
    }

    public int rowCount() {
        return rows.size();
    }

    /** Lay out the table so its lines fit within {@code maxWidth} characters. */
    public RenderedTable render(int maxWidth) {
        if (columns.isEmpty()) {
            return new RenderedTable("", "", List.of());
        }
        TableLayout layout = layout(maxWidth);
        String header = String.join(COLUMN_SEP, layout.header());
        String separator = joinSeparator(layout.widths());
        List<String> rowLines = new ArrayList<>(layout.rows().size());
        for (List<String> cells : layout.rows()) {
            rowLines.add(String.join(COLUMN_SEP, cells));
        }
        return new RenderedTable(header, separator, rowLines);
    }

    /**
     * Lay out the table into fitted cells: per-column widths, the fitted header
     * cells, and the fitted body cells. Lets callers render with per-cell control
     * (e.g. highlighting a single selected cell).
     */
    public TableLayout layout(int maxWidth) {
        int n = columns.size();
        int[] width = new int[n];
        for (int c = 0; c < n; c++) {
            width[c] = Math.max(1, columns.get(c).name().length());
        }
        for (List<String> row : rows) {
            for (int c = 0; c < n; c++) {
                width[c] = Math.max(width[c], cell(row, c).length());
            }
        }
        int overhead = Math.max(0, (n - 1)) * COLUMN_SEP.length();
        int budget = Math.max(Math.max(1, n), maxWidth - overhead);
        shrinkToFit(width, budget);

        List<String> header = new ArrayList<>(n);
        for (int c = 0; c < n; c++) {
            header.add(fit(columns.get(c).name(), width[c], columns.get(c).align()));
        }
        List<List<String>> bodyCells = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            List<String> fitted = new ArrayList<>(n);
            for (int c = 0; c < n; c++) {
                fitted.add(fit(cell(row, c), width[c], columns.get(c).align()));
            }
            bodyCells.add(fitted);
        }
        return new TableLayout(width, header, bodyCells);
    }

    /** The column separator used between cells (also the cell-boundary stride). */
    public static String columnSeparator() {
        return COLUMN_SEP;
    }

    private static String cell(List<String> row, int index) {
        if (row == null || index >= row.size() || row.get(index) == null) {
            return "";
        }
        return row.get(index);
    }

    private static void shrinkToFit(int[] width, int budget) {
        int sum = 0;
        for (int w : width) {
            sum += w;
        }
        while (sum > budget) {
            int widest = -1;
            for (int c = 0; c < width.length; c++) {
                if (width[c] > 1 && (widest < 0 || width[c] > width[widest])) {
                    widest = c;
                }
            }
            if (widest < 0) {
                break; // every column already at minimum width
            }
            width[widest]--;
            sum--;
        }
    }

    private static String joinSeparator(int[] width) {
        StringBuilder builder = new StringBuilder();
        for (int c = 0; c < width.length; c++) {
            if (c > 0) {
                builder.append(SEP_JOINT);
            }
            builder.append("-".repeat(width[c]));
        }
        return builder.toString();
    }

    private static String fit(String value, int width, Align align) {
        String text = value == null ? "" : value;
        if (text.length() > width) {
            return width <= 1 ? ELLIPSIS : text.substring(0, width - 1) + ELLIPSIS;
        }
        int pad = width - text.length();
        return switch (align) {
        case LEFT -> text + " ".repeat(pad);
        case RIGHT -> " ".repeat(pad) + text;
        case CENTER -> {
            int left = pad / 2;
            yield " ".repeat(left) + text + " ".repeat(pad - left);
        }
        };
    }
}
