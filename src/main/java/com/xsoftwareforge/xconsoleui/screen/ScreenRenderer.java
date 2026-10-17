package com.xsoftwareforge.xconsoleui.screen;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xsoftwareforge.xconsoleui.config.MenuColor;
import com.xsoftwareforge.xconsoleui.config.MenuConfig;
import com.xsoftwareforge.xconsoleui.config.MenuTheme;
import com.xsoftwareforge.xconsoleui.config.MenuTheme.Slot;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.OptionColor;
import com.xsoftwareforge.xconsoleui.table.RenderedTable;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableLayout;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

/**
 * Renders the interactive screens of {@link JLineTerminalScreen} to a JLine
 * {@link Terminal}: clears the screen, draws titled selections, grids, tables,
 * forms and result views, and applies the configured {@link MenuTheme} colours.
 * <p>
 * This is the output half of the screen; the input half (key decoding) lives in
 * {@link KeyParser}. The renderer is stateless beyond its terminal and config —
 * every method receives the already-computed layout values (scroll offset, visible
 * row count, column widths) from the orchestrating loop, so layout math is never
 * duplicated here.
 */
final class ScreenRenderer {

    private static final String HELP_SELECT = "Arrows/PgUp/PgDn/Home/End to navigate, Enter to confirm, Esc or q to go back.";
    private static final String HELP_MULTI = "Arrows/PgUp/PgDn to move, Space to toggle, Enter to confirm, Esc or q to cancel.";
    private static final String HELP_INPUT = "Type your value, Backspace deletes, Enter confirms, Esc goes back.";
    private static final String HELP_RESULT = "Arrows/Page/Home/End to scroll, Enter or Esc (or q) to go back.";
    private static final String HELP_EMPTY = "No entries here. Press Enter or Esc to go back.";

    private final Terminal terminal;
    private final MenuConfig config;

    ScreenRenderer(Terminal terminal, MenuConfig config) {
        this.terminal = terminal;
        this.config = config;
    }

    void clear() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.puts(InfoCmp.Capability.cursor_home);
    }

    void renderSelection(String title, List<MenuItem> options, int selectedIndex, int scrollOffset,
            int visibleRows, Map<FunctionKey, String> hotkeys) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_SELECT).toAnsi(terminal));
        if (hotkeys != null && !hotkeys.isEmpty()) {
            terminal.writer().println(styleHotkeys(formatHotkeys(hotkeys)).toAnsi(terminal));
        }
        terminal.writer().println();
        for (int index = scrollOffset; index < Math.min(options.size(), scrollOffset + visibleRows); index++) {
            MenuItem option = options.get(index);
            terminal.writer().println((index == selectedIndex
                    ? styleSelected("> " + option.label(), option.color())
                    : styleOption("  " + option.label(), option.color())).toAnsi(terminal));
        }
        terminal.writer().println();
        terminal.writer().println(styleFooter(selectionFooter(selectedIndex, options.size(), visibleRows))
                .toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderSelectionGrid(String title, List<MenuItem> options, int selected, int columns, int rows,
            int scroll, int visibleRows, int colWidth, int gap, Map<FunctionKey, String> hotkeys) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_SELECT).toAnsi(terminal));
        if (!hotkeys.isEmpty()) {
            terminal.writer().println(styleHotkeys(formatHotkeys(hotkeys)).toAnsi(terminal));
        }
        terminal.writer().println();
        int end = Math.min(rows, scroll + visibleRows);
        for (int r = scroll; r < end; r++) {
            AttributedStringBuilder line = new AttributedStringBuilder();
            for (int c = 0; c < columns; c++) {
                int index = c * rows + r;
                if (index >= options.size()) {
                    break;
                }
                if (c > 0) {
                    line.append(" ".repeat(gap));
                }
                MenuItem option = options.get(index);
                String label = fitLabel(option.label(), colWidth);
                line.append(index == selected ? styleSelected(label, option.color())
                        : styleOption(label, option.color()));
            }
            terminal.writer().println(line.toAttributedString().toAnsi(terminal));
        }
        terminal.writer().println();
        terminal.writer().println(styleFooter("Item " + (selected + 1) + " of " + options.size()
                + "  (grid: " + columns + " cols)").toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderMultiSelection(String title, List<MenuItem> options, Set<String> selected, int selectedIndex,
            int scrollOffset, int visibleRows) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_MULTI).toAnsi(terminal));
        terminal.writer().println();
        for (int index = scrollOffset; index < Math.min(options.size(), scrollOffset + visibleRows); index++) {
            MenuItem option = options.get(index);
            boolean checked = selected.contains(option.id());
            String prefix = checked ? "[x] " : "[ ] ";
            String line = (index == selectedIndex ? "> " : "  ") + prefix + option.label();
            terminal.writer().println((index == selectedIndex
                    ? styleSelected(line, option.color())
                    : styleOption(line, option.color())).toAnsi(terminal));
        }
        terminal.writer().println();
        String footer = "Selected " + selected.size() + " item(s)";
        int totalPages = pageCount(options.size(), visibleRows);
        if (totalPages > 1) {
            footer = "Page " + (selectedIndex / visibleRows + 1) + " of " + totalPages + "  -  " + footer;
        }
        terminal.writer().println(styleFooter(footer).toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderInput(String title, String currentValue, String defaultValue, boolean masked) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_INPUT).toAnsi(terminal));
        terminal.writer().println();
        if (defaultValue != null && !defaultValue.isBlank()) {
            terminal.writer().println(styleFooter("Default: " + defaultValue).toAnsi(terminal));
        }
        terminal.writer().println();
        terminal.writer().println(styleSelected("> " + (masked ? "*".repeat(currentValue.length()) : currentValue))
                .toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderConfirm(String title, boolean selectedYes) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_SELECT).toAnsi(terminal));
        terminal.writer().println();
        terminal.writer().println((selectedYes ? styleSelected("> Yes") : styleOption("  Yes")).toAnsi(terminal));
        terminal.writer().println((selectedYes ? styleOption("  No") : styleSelected("> No")).toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderResult(String title, List<String> lines, int scrollOffset, int visibleRows) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_RESULT).toAnsi(terminal));
        terminal.writer().println();
        if (lines.isEmpty()) {
            terminal.writer().println(styleFooter("(no details)").toAnsi(terminal));
        } else {
            for (int index = scrollOffset; index < Math.min(lines.size(), scrollOffset + visibleRows); index++) {
                terminal.writer().println(styleContent(lines.get(index)).toAnsi(terminal));
            }
        }
        terminal.writer().println();
        terminal.writer().println(styleFooter("Lines " + (lines.isEmpty() ? 0 : scrollOffset + 1) + "-"
                + (lines.isEmpty() ? 0 : Math.min(lines.size(), scrollOffset + visibleRows))
                + " of " + lines.size()).toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderPlaceholder(String title, Map<FunctionKey, String> hotkeys) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint(HELP_EMPTY).toAnsi(terminal));
        if (hotkeys != null && !hotkeys.isEmpty()) {
            terminal.writer().println(styleHotkeys(formatHotkeys(hotkeys)).toAnsi(terminal));
        }
        terminal.writer().println();
        terminal.writer().println(styleFooter("(no entries)").toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderTable(String title, RenderedTable rendered, List<String> content, int scroll, int visibleRows,
            boolean pinHeader, boolean headerVisible, boolean exportEnabled) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint("Arrows/Page/Home/End scroll, h header, "
                + (exportEnabled ? "e export, " : "") + "Esc or q to go back.").toAnsi(terminal));
        terminal.writer().println();
        if (pinHeader) {
            terminal.writer().println(styleTableHeader(rendered.header()).toAnsi(terminal));
            terminal.writer().println(styleContent(rendered.separator()).toAnsi(terminal));
        }
        int end = Math.min(content.size(), scroll + visibleRows);
        for (int i = scroll; i < end; i++) {
            terminal.writer().println(styleContent(content.get(i)).toAnsi(terminal));
        }
        terminal.writer().println();
        String footer = "Rows " + (content.isEmpty() ? 0 : scroll + 1) + "-" + end + " of " + content.size()
                + (headerVisible ? "" : "  (header hidden)");
        terminal.writer().println(styleFooter(footer).toAnsi(terminal));
        terminal.writer().flush();
    }

    void renderTableCells(String title, TableLayout layout, List<String> joined, List<Integer> filtered,
            int selected, int col, int scroll, int visibleRows, String filter, boolean filterable,
            Map<FunctionKey, String> actions, boolean cellMode) {
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint("Tab " + (cellMode ? "row mode" : "cell mode")
                + ", arrows move, " + (cellMode ? "Left/Right cell, Enter edit, " : "Enter select, ")
                + (filterable ? "type to filter, " : "") + "Esc back.").toAnsi(terminal));
        if (!actions.isEmpty()) {
            terminal.writer().println(styleHotkeys(formatHotkeys(actions)).toAnsi(terminal));
        }
        if (filterable) {
            terminal.writer().println(styleHint("Filter: " + filter + "_").toAnsi(terminal));
        }
        terminal.writer().println(styleTableHeader("  " + String.join(Table.columnSeparator(), layout.header()))
                .toAnsi(terminal));
        terminal.writer().println();
        int end = Math.min(filtered.size(), scroll + visibleRows);
        for (int i = scroll; i < end; i++) {
            int rowIndex = filtered.get(i);
            boolean rowSelected = i == selected;
            if (cellMode && rowSelected) {
                terminal.writer().println(renderCellRow(layout.rows().get(rowIndex), col).toAnsi(terminal));
            } else {
                String line = "  " + joined.get(rowIndex);
                terminal.writer().println((rowSelected ? styleSelected("> " + joined.get(rowIndex))
                        : styleOption(line)).toAnsi(terminal));
            }
        }
        terminal.writer().println();
        String footer = filtered.isEmpty() ? "No matches"
                : "Row " + (selected + 1) + " of " + filtered.size()
                        + (cellMode ? "  Col " + (col + 1) + " of " + layout.widths().length : "")
                        + (cellMode ? "  [cell mode]" : "  [row mode]");
        terminal.writer().println(styleFooter(footer).toAnsi(terminal));
        terminal.writer().flush();
    }

    private AttributedString renderCellRow(List<String> cells, int selectedCol) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append(styleSelected("> "));
        for (int c = 0; c < cells.size(); c++) {
            if (c > 0) {
                builder.append(styleOption(Table.columnSeparator()));
            }
            builder.append(c == selectedCol ? styleSelected(cells.get(c)) : styleOption(cells.get(c)));
        }
        return builder.toAttributedString();
    }

    /** Width of the {@code "[x] "} mark column of a multi-select browser. */
    static final int MARK_WIDTH = 4;

    /**
     * @param marked marked original row indices of a multi-select browser, or {@code null}
     *               for a plain browser (no mark column)
     */
    void renderBrowse(String title, List<String> head, List<String> rows, List<Integer> filtered,
            int selected, int scroll, int visibleRows, String filter, boolean filterable,
            Map<FunctionKey, String> actions, Set<Integer> marked) {
        boolean multi = marked != null;
        clear();
        terminal.writer().println(styleTitle(title).toAnsi(terminal));
        terminal.writer().println(styleHint("Arrows navigate, Enter select, "
                + (multi ? "Space mark, Ctrl+A mark all, " : "")
                + (filterable ? "type to filter, " : "") + "Esc " + (multi ? "unmark/back." : "back."))
                .toAnsi(terminal));
        if (!actions.isEmpty()) {
            terminal.writer().println(styleHotkeys(formatHotkeys(actions)).toAnsi(terminal));
        }
        if (filterable) {
            terminal.writer().println(styleHint("Filter: " + filter + "_").toAnsi(terminal));
        }
        String headIndent = multi ? " ".repeat(MARK_WIDTH) : "";
        for (String line : head) {
            terminal.writer().println(styleTableHeader(headIndent + line).toAnsi(terminal));
        }
        terminal.writer().println();
        int end = Math.min(filtered.size(), scroll + visibleRows);
        for (int i = scroll; i < end; i++) {
            int index = filtered.get(i);
            boolean isMarked = multi && marked.contains(index);
            String line = (multi ? (isMarked ? "[x] " : "[ ] ") : "") + rows.get(index);
            AttributedString styled;
            if (i == selected) {
                styled = styleSelected("> " + line);
            } else if (isMarked) {
                styled = styleHotkeys("  " + line); // marked rows stand out in the theme's hotkey colour
            } else {
                styled = styleOption("  " + line);
            }
            terminal.writer().println(styled.toAnsi(terminal));
        }
        terminal.writer().println();
        String footer = filtered.isEmpty()
                ? "No matches"
                : "Item " + (selected + 1) + " of " + filtered.size()
                        + (filtered.size() != rows.size() ? " (of " + rows.size() + ")" : "");
        if (multi) {
            footer += "  -  " + marked.size() + " markiert";
        }
        terminal.writer().println(styleFooter(footer).toAnsi(terminal));
        terminal.writer().flush();
    }

    private static String fitLabel(String label, int width) {
        if (label.length() > width) {
            return width <= 1 ? "…" : label.substring(0, width - 1) + "…";
        }
        return label + " ".repeat(width - label.length());
    }

    private static String formatHotkeys(Map<FunctionKey, String> hotkeys) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<FunctionKey, String> entry : hotkeys.entrySet()) {
            if (builder.length() > 0) {
                builder.append("   ");
            }
            builder.append(entry.getKey()).append(' ').append(entry.getValue());
        }
        return builder.toString();
    }

    private static int pageCount(int total, int visibleRows) {
        if (visibleRows <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) total / visibleRows));
    }

    private static String selectionFooter(int selectedIndex, int total, int visibleRows) {
        int totalPages = pageCount(total, visibleRows);
        if (totalPages <= 1) {
            return "Item " + (selectedIndex + 1) + " of " + total;
        }
        int currentPage = selectedIndex / visibleRows + 1;
        return "Page " + currentPage + " of " + totalPages + "  -  item " + (selectedIndex + 1) + " of " + total;
    }

    private MenuTheme theme() {
        return config.theme();
    }

    private AttributedString styleTitle(String value) {
        return styled(value, AttributedStyle.DEFAULT.bold(), theme().color(Slot.TITLE), MenuColor.DEFAULT);
    }

    private AttributedString styleHint(String value) {
        return styled(value, AttributedStyle.DEFAULT, theme().color(Slot.HINT), MenuColor.DEFAULT);
    }

    private AttributedString styleHotkeys(String value) {
        return styled(value, AttributedStyle.DEFAULT, theme().color(Slot.HOTKEY), MenuColor.DEFAULT);
    }

    private AttributedString styleSelected(String value) {
        return styleSelected(value, OptionColor.DEFAULT);
    }

    private AttributedString styleSelected(String value, OptionColor color) {
        MenuColor foreground = color == OptionColor.RED ? MenuColor.RED : theme().color(Slot.SELECTED_FOREGROUND);
        return styled(value, AttributedStyle.DEFAULT.bold(), foreground, theme().color(Slot.SELECTED_BACKGROUND));
    }

    private AttributedString styleOption(String value) {
        return styleOption(value, OptionColor.DEFAULT);
    }

    private AttributedString styleOption(String value, OptionColor color) {
        return styled(value, AttributedStyle.DEFAULT, optionForeground(color), MenuColor.DEFAULT);
    }

    private MenuColor optionForeground(OptionColor color) {
        return switch (color == null ? OptionColor.DEFAULT : color) {
        case DEFAULT -> theme().color(Slot.OPTION);
        case GREEN -> MenuColor.GREEN;
        case RED -> MenuColor.RED;
        case WHITE -> MenuColor.WHITE;
        };
    }

    private AttributedString styleContent(String value) {
        return styled(value, AttributedStyle.DEFAULT, theme().color(Slot.CONTENT), MenuColor.DEFAULT);
    }

    private AttributedString styleTableHeader(String value) {
        return styled(value, AttributedStyle.DEFAULT.bold(), theme().color(Slot.OPTION), MenuColor.DEFAULT);
    }

    private AttributedString styleFooter(String value) {
        return styled(value, AttributedStyle.DEFAULT.bold(), theme().color(Slot.FOOTER), MenuColor.DEFAULT);
    }

    private AttributedString styled(String value, AttributedStyle base, MenuColor foreground, MenuColor background) {
        AttributedStyle style = base;
        if (foreground != MenuColor.DEFAULT) {
            style = style.foreground(toAnsi(foreground));
        }
        if (background != MenuColor.DEFAULT) {
            style = style.background(toAnsi(background));
        }
        return new AttributedStringBuilder().style(style).append(value).toAttributedString();
    }

    private static int toAnsi(MenuColor color) {
        return switch (color) {
        case BLACK -> AttributedStyle.BLACK;
        case RED -> AttributedStyle.RED;
        case GREEN -> AttributedStyle.GREEN;
        case YELLOW -> AttributedStyle.YELLOW;
        case BLUE -> AttributedStyle.BLUE;
        case MAGENTA -> AttributedStyle.MAGENTA;
        case CYAN -> AttributedStyle.CYAN;
        case WHITE, DEFAULT -> AttributedStyle.WHITE;
        };
    }
}
