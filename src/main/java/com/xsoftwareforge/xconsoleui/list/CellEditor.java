package com.xsoftwareforge.xconsoleui.list;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/**
 * Edits a single column's cell of a row in {@link TableBrowser} cell mode. Runs
 * the full edit (prompt + write-back into the row object), so it owns the input
 * type and validation.
 *
 * @param <T> the row element type
 */
@FunctionalInterface
public interface CellEditor<T> {

    void edit(MenuContext context, T item) throws Exception;
}
