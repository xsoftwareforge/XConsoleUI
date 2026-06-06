package com.xsoftwareforge.xconsoleui.table;

import java.util.List;

/**
 * A table laid out to a width as fitted cells: per-column {@code widths}, the
 * fitted {@code header} cells, and the fitted body {@code rows} (each a list of
 * cell strings already padded/truncated to the column width). Lets a renderer
 * highlight an individual cell.
 */
public record TableLayout(int[] widths, List<String> header, List<List<String>> rows) {
}
