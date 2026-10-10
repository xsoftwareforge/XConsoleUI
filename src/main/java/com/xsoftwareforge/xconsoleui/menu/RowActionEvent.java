package com.xsoftwareforge.xconsoleui.menu;

/**
 * Reported once a row action of a browser has returned (see
 * {@link MenuContext#rowActionCompleted}).
 *
 * @param browserTitle title of the browser the action ran in
 * @param row          display text of the chosen row ({@code RowBrowser}) or its first cell
 *                     ({@code TableBrowser})
 * @param actionKey    function key of the action, or {@code null} for Enter (select handler,
 *                     cell edit)
 * @param actionLabel  hint label of the action; {@code "Select"} for an Enter handler,
 *                     {@code "Edit <column>"} for a cell edit
 * @param error        what the action threw, or {@code null} when it completed normally
 */
public record RowActionEvent(String browserTitle, String row, FunctionKey actionKey, String actionLabel,
        Throwable error) {
}
