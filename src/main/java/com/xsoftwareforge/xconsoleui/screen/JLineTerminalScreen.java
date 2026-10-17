package com.xsoftwareforge.xconsoleui.screen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.xsoftwareforge.xconsoleui.config.ListViewMode;
import com.xsoftwareforge.xconsoleui.config.MenuConfig;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.BrowseState;
import com.xsoftwareforge.xconsoleui.menu.CellBrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.TableCellResult;
import com.xsoftwareforge.xconsoleui.table.RenderedTable;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableLayout;
import com.xsoftwareforge.xconsoleui.table.TableViewAction;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

import com.xsoftwareforge.xconsoleui.menu.MenuItem;

/**
 * JLine-backed {@link TerminalScreen}. Switches the terminal into raw mode,
 * renders the requested screen, reads cursor keys (arrows, Home/End, Page
 * Up/Down, Enter, Esc) and restores the previous terminal attributes afterwards.
 * <p>
 * Terminal resizes are handled live: a {@code WINCH} signal handler flags the
 * change and the active screen is re-rendered with the new dimensions on the next
 * poll cycle, so the visible page size always matches the current window height.
 */
public class JLineTerminalScreen implements TerminalScreen {

    /** Fallback width used when the terminal does not report a usable column count. */
    private static final int DEFAULT_TERMINAL_WIDTH = 80;

    /** Spaces inserted between columns of a multi-column (grid) layout. */
    private static final int GRID_COLUMN_GAP = 2;

    private final Terminal terminal;
    private final MenuConfig config;
    private final AtomicBoolean resizePending = new AtomicBoolean();
    private final KeyParser keys = new KeyParser(resizePending);
    private final ScreenRenderer renderer;

    public JLineTerminalScreen(Terminal terminal) {
        this(terminal, MenuConfig.defaults());
    }

    public JLineTerminalScreen(Terminal terminal, MenuConfig config) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.config = Objects.requireNonNull(config, "config");
        this.renderer = new ScreenRenderer(this.terminal, this.config);
    }

    @Override
    public String select(String title, List<MenuItem> options) throws Exception {
        ScreenChoice choice = select(title, options, Map.of(), false);
        return choice.isItem() ? choice.itemId() : null;
    }

    @Override
    public ScreenChoice select(String title, List<MenuItem> options, Map<FunctionKey, String> hotkeys, boolean grid)
            throws Exception {
        Map<FunctionKey, String> activeHotkeys = hotkeys == null ? Map.of() : hotkeys;
        if (options == null || options.isEmpty()) {
            return selectEmpty(title, activeHotkeys);
        }
        if (grid && options.size() > 1) {
            return selectGrid(title, options, activeHotkeys);
        }
        int reserved = activeHotkeys.isEmpty() ? 6 : 7;
        return withRawScreen(reader -> {
            int selectedIndex = 0;
            int scrollOffset = 0;
            while (true) {
                int visibleRows = visibleRows(reserved);
                scrollOffset = clampScroll(selectedIndex, scrollOffset, visibleRows);
                renderer.renderSelection(title, options, selectedIndex, scrollOffset, visibleRows, activeHotkeys);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> selectedIndex = Math.max(0, selectedIndex - 1);
                case DOWN -> selectedIndex = Math.min(options.size() - 1, selectedIndex + 1);
                case PAGE_UP -> selectedIndex = Math.max(0, selectedIndex - visibleRows);
                case PAGE_DOWN -> selectedIndex = Math.min(options.size() - 1, selectedIndex + visibleRows);
                case HOME -> selectedIndex = 0;
                case END -> selectedIndex = options.size() - 1;
                case ENTER -> {
                    return ScreenChoice.item(options.get(selectedIndex).id());
                }
                case ESCAPE -> {
                    return ScreenChoice.back();
                }
                case CHARACTER -> {
                    if (keys.isQuitCharacter()) {
                        return ScreenChoice.back();
                    }
                }
                case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> {
                    FunctionKey pressed = toFunctionKey(key);
                    if (pressed == config.viewToggleKey()) {
                        return ScreenChoice.toggleView();
                    }
                    if (activeHotkeys.containsKey(pressed)) {
                        return ScreenChoice.hotkey(pressed);
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    private ScreenChoice selectGrid(String title, List<MenuItem> options, Map<FunctionKey, String> hotkeys)
            throws Exception {
        int gap = GRID_COLUMN_GAP;
        int reserved = 6 + (hotkeys.isEmpty() ? 0 : 1);
        return withRawScreen(reader -> {
            int selected = 0;
            int scroll = 0;
            while (true) {
                int width = terminalWidth();
                int colWidth = maxLabelWidth(options, width);
                int columns = Math.max(1, (width + gap) / (colWidth + gap));
                int rows = (options.size() + columns - 1) / columns;
                int selectedRow = selected % rows;
                int visibleRows = visibleRows(reserved);
                scroll = clampScroll(selectedRow, scroll, visibleRows);
                renderer.renderSelectionGrid(title, options, selected, columns, rows, scroll, visibleRows, colWidth,
                        gap, hotkeys);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> selected = Math.max(0, selected - 1);
                case DOWN -> selected = Math.min(options.size() - 1, selected + 1);
                case LEFT -> selected = Math.max(0, selected - rows);
                case RIGHT -> selected = Math.min(options.size() - 1, selected + rows);
                case PAGE_UP -> selected = Math.max(0, selected - visibleRows);
                case PAGE_DOWN -> selected = Math.min(options.size() - 1, selected + visibleRows);
                case HOME -> selected = 0;
                case END -> selected = options.size() - 1;
                case ENTER -> {
                    return ScreenChoice.item(options.get(selected).id());
                }
                case ESCAPE -> {
                    return ScreenChoice.back();
                }
                case CHARACTER -> {
                    if (keys.isQuitCharacter()) {
                        return ScreenChoice.back();
                    }
                }
                case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> {
                    FunctionKey pressed = toFunctionKey(key);
                    if (pressed == config.viewToggleKey()) {
                        return ScreenChoice.toggleView();
                    }
                    if (hotkeys.containsKey(pressed)) {
                        return ScreenChoice.hotkey(pressed);
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    private static int maxLabelWidth(List<MenuItem> options, int width) {
        int maxLen = 1;
        for (MenuItem option : options) {
            maxLen = Math.max(maxLen, option.label().length());
        }
        return Math.max(1, Math.min(maxLen, width));
    }

    private ScreenChoice selectEmpty(String title, Map<FunctionKey, String> hotkeys) throws Exception {
        return withRawScreen(reader -> {
            while (true) {
                renderer.renderPlaceholder(title, hotkeys);
                Key key = keys.readKey(reader);
                switch (key) {
                case ENTER, ESCAPE -> {
                    return ScreenChoice.back();
                }
                case CHARACTER -> {
                    if (keys.isQuitCharacter()) {
                        return ScreenChoice.back();
                    }
                }
                case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> {
                    FunctionKey pressed = toFunctionKey(key);
                    if (hotkeys.containsKey(pressed)) {
                        return ScreenChoice.hotkey(pressed);
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    private static FunctionKey toFunctionKey(Key key) {
        return switch (key) {
        case F1 -> FunctionKey.F1;
        case F2 -> FunctionKey.F2;
        case F3 -> FunctionKey.F3;
        case F4 -> FunctionKey.F4;
        case F5 -> FunctionKey.F5;
        case F6 -> FunctionKey.F6;
        case F7 -> FunctionKey.F7;
        case F8 -> FunctionKey.F8;
        case F9 -> FunctionKey.F9;
        case F10 -> FunctionKey.F10;
        case F11 -> FunctionKey.F11;
        case F12 -> FunctionKey.F12;
        default -> null;
        };
    }

    @Override
    public List<String> selectMultiple(String title, List<MenuItem> options, List<String> selectedIds) throws Exception {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        Set<String> selected = new LinkedHashSet<>();
        if (selectedIds != null) {
            selected.addAll(selectedIds);
        }
        return withRawScreen(reader -> {
            int selectedIndex = 0;
            int scrollOffset = 0;
            while (true) {
                int visibleRows = visibleRows(6);
                scrollOffset = clampScroll(selectedIndex, scrollOffset, visibleRows);
                renderer.renderMultiSelection(title, options, selected, selectedIndex, scrollOffset, visibleRows);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> selectedIndex = Math.max(0, selectedIndex - 1);
                case DOWN -> selectedIndex = Math.min(options.size() - 1, selectedIndex + 1);
                case PAGE_UP -> selectedIndex = Math.max(0, selectedIndex - visibleRows);
                case PAGE_DOWN -> selectedIndex = Math.min(options.size() - 1, selectedIndex + visibleRows);
                case HOME -> selectedIndex = 0;
                case END -> selectedIndex = options.size() - 1;
                case CHARACTER -> {
                    Character printable = keys.lastCharacter();
                    if (keys.isQuitCharacter()) {
                        return null;
                    }
                    if (printable != null && printable == ' ') {
                        String optionId = options.get(selectedIndex).id();
                        if (!selected.remove(optionId)) {
                            selected.add(optionId);
                        }
                    }
                }
                case ENTER -> {
                    List<String> orderedSelection = new ArrayList<>();
                    for (MenuItem option : options) {
                        if (selected.contains(option.id())) {
                            orderedSelection.add(option.id());
                        }
                    }
                    return orderedSelection;
                }
                case ESCAPE -> {
                    return null;
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    @Override
    public String input(String title, String defaultValue, Character mask) throws Exception {
        return withRawScreen(reader -> {
            StringBuilder buffer = new StringBuilder();
            while (true) {
                renderer.renderInput(title, buffer.toString(), defaultValue, mask != null);
                Key key = keys.readKey(reader);
                switch (key) {
                case ENTER -> {
                    if (buffer.length() == 0 && defaultValue != null) {
                        return defaultValue;
                    }
                    return buffer.toString();
                }
                case BACKSPACE -> {
                    if (buffer.length() > 0) {
                        buffer.deleteCharAt(buffer.length() - 1);
                    }
                }
                case ESCAPE -> {
                    return null;
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                case CHARACTER -> {
                    Character printable = keys.lastCharacter();
                    if (printable != null && !Character.isISOControl(printable)) {
                        buffer.append(printable);
                    }
                }
                default -> {
                }
                }
            }
        });
    }

    @Override
    public boolean confirm(String title, boolean defaultValue) throws Exception {
        return withRawScreen(reader -> {
            boolean selectedYes = defaultValue;
            while (true) {
                renderer.renderConfirm(title, selectedYes);
                Key key = keys.readKey(reader);
                switch (key) {
                case UP, DOWN, LEFT, RIGHT -> selectedYes = !selectedYes;
                case ENTER -> {
                    return selectedYes;
                }
                case ESCAPE -> {
                    return false;
                }
                case CHARACTER -> {
                    if (keys.isQuitCharacter()) {
                        return false;
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    @Override
    public void showResult(String title, List<String> lines, boolean grid) throws Exception {
        List<String> items = lines == null ? List.of() : lines;
        withRawScreen(reader -> {
            int scrollOffset = 0;
            boolean gridMode = grid;
            while (true) {
                int width = terminalWidth();
                List<String> display = gridMode ? composeGrid(items, width) : items;
                int visibleRows = visibleRows(6);
                int maxOffset = Math.max(0, display.size() - visibleRows);
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
                renderer.renderResult(title, display, scrollOffset, visibleRows);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> scrollOffset = Math.max(0, scrollOffset - 1);
                case DOWN -> scrollOffset = Math.min(maxOffset, scrollOffset + 1);
                case PAGE_UP -> scrollOffset = Math.max(0, scrollOffset - visibleRows);
                case PAGE_DOWN -> scrollOffset = Math.min(maxOffset, scrollOffset + visibleRows);
                case HOME -> scrollOffset = 0;
                case END -> scrollOffset = maxOffset;
                case ENTER, ESCAPE -> {
                    return null;
                }
                case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> {
                    if (toFunctionKey(key) == config.viewToggleKey()) {
                        gridMode = !gridMode;
                        scrollOffset = 0;
                    }
                }
                case CHARACTER -> {
                    if (keys.isQuitCharacter()) {
                        return null;
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    /** Arrange {@code items} into column-major columns that fit {@code width}. */
    static List<String> composeGrid(List<String> items, int width) {
        if (items.isEmpty()) {
            return List.of();
        }
        int gap = GRID_COLUMN_GAP;
        int maxLen = 0;
        for (String item : items) {
            maxLen = Math.max(maxLen, item == null ? 0 : item.length());
        }
        int colWidth = Math.max(1, Math.min(maxLen, width));
        int columns = Math.max(1, (width + gap) / (colWidth + gap));
        if (columns <= 1) {
            return List.copyOf(items);
        }
        int rows = (items.size() + columns - 1) / columns;
        List<String> lines = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < columns; c++) {
                int index = c * rows + r;
                if (index >= items.size()) {
                    break;
                }
                if (c > 0) {
                    line.append(" ".repeat(gap));
                }
                String item = items.get(index);
                if (item == null) {
                    item = "";
                }
                if (item.length() > colWidth) {
                    item = item.substring(0, colWidth);
                }
                boolean last = index + rows >= items.size();
                line.append(last ? item : padRight(item, colWidth));
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static String padRight(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    @Override
    public TableViewAction showTable(String title, Table table, boolean stickyHeader, boolean exportEnabled)
            throws Exception {
        return withRawScreen(reader -> {
            boolean headerVisible = true;
            int scroll = 0;
            while (true) {
                int width = terminalWidth();
                RenderedTable rendered = table.render(width);
                boolean pinHeader = headerVisible && stickyHeader;

                List<String> content = new ArrayList<>();
                if (headerVisible && !stickyHeader) {
                    content.add(rendered.header());
                    content.add(rendered.separator());
                }
                content.addAll(rendered.rows());

                int reserved = 6 + (pinHeader ? 2 : 0);
                int visibleRows = visibleRows(reserved);
                int maxOffset = Math.max(0, content.size() - visibleRows);
                scroll = Math.max(0, Math.min(scroll, maxOffset));
                renderer.renderTable(title, rendered, content, scroll, visibleRows, pinHeader, headerVisible,
                        exportEnabled);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> scroll = Math.max(0, scroll - 1);
                case DOWN -> scroll = Math.min(maxOffset, scroll + 1);
                case PAGE_UP -> scroll = Math.max(0, scroll - visibleRows);
                case PAGE_DOWN -> scroll = Math.min(maxOffset, scroll + visibleRows);
                case HOME -> scroll = 0;
                case END -> scroll = maxOffset;
                case ENTER, ESCAPE -> {
                    return TableViewAction.BACK;
                }
                case CHARACTER -> {
                    Character ch = keys.lastCharacter();
                    if (keys.isQuitCharacter()) {
                        return TableViewAction.BACK;
                    }
                    if (ch != null && (ch == 'h' || ch == 'H')) {
                        headerVisible = !headerVisible;
                        scroll = 0;
                    } else if (exportEnabled && ch != null && (ch == 'e' || ch == 'E')) {
                        return TableViewAction.EXPORT;
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        });
    }

    @Override
    public BrowseResult browse(String title, List<String> header, List<String> rows,
            Map<FunctionKey, String> rowActions, boolean filterable) throws Exception {
        return browse(title, header, rows, rowActions, filterable, null);
    }

    @Override
    public BrowseResult browse(String title, List<String> header, List<String> rows,
            Map<FunctionKey, String> rowActions, boolean filterable, BrowseState state) throws Exception {
        List<String> allRows = rows == null ? List.of() : rows;
        List<String> head = header == null ? List.of() : header;
        Map<FunctionKey, String> actions = rowActions == null ? Map.of() : rowActions;
        return withRawScreen(reader ->
                runRowBrowser(reader, title, () -> new BrowseContent(head, allRows), actions, filterable, state));
    }

    @Override
    public BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable) throws Exception {
        return browseTable(title, table, rowActions, filterable, null);
    }

    @Override
    public BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable, BrowseState state) throws Exception {
        Map<FunctionKey, String> actions = rowActions == null ? Map.of() : rowActions;
        // The "[x] " mark column takes MARK_WIDTH characters off the table width.
        int markWidth = state == null ? 0 : ScreenRenderer.MARK_WIDTH;
        return withRawScreen(reader -> runRowBrowser(reader, title, () -> {
            int width = Math.max(1, terminalWidth() - markWidth);
            RenderedTable rendered = table.render(width);
            return new BrowseContent(List.of(rendered.header(), rendered.separator()), rendered.rows());
        }, actions, filterable, state));
    }

    /** Ctrl+A arrives as the raw control character 0x01. */
    private static final char CTRL_A = 1;

    /**
     * Row browser loop. With a {@code state} the browser is multi-select: Space and
     * Ctrl+A mark rows instead of extending the filter (Space therefore cannot be
     * typed into the filter), and Esc clears marks first. Without one it behaves
     * exactly as in 1.0.
     */
    private BrowseResult runRowBrowser(NonBlockingReader reader, String title, BrowseContentProvider provider,
            Map<FunctionKey, String> actions, boolean filterable, BrowseState state) throws IOException {
        boolean multi = state != null;
        Set<Integer> marked = multi ? state.marked() : null;
        StringBuilder filter = new StringBuilder(multi && filterable ? state.filter() : "");
        boolean restore = multi;
        int selected = 0;
        int scroll = 0;
        List<Integer> filtered = List.of();
        try {
            while (true) {
                BrowseContent content = provider.get();
                List<String> head = content.header();
                List<String> allRows = content.rows();
                filtered = filterRows(allRows, filter.toString());
                if (restore) {
                    selected = indexOfOriginal(filtered, state.row());
                    restore = false;
                }
                if (selected >= filtered.size()) {
                    selected = Math.max(0, filtered.size() - 1);
                }
                int reserved = 5 + head.size() + (filterable ? 1 : 0) + (actions.isEmpty() ? 0 : 1);
                int visibleRows = visibleRows(reserved);
                scroll = clampScroll(selected, scroll, visibleRows);
                renderer.renderBrowse(title, head, allRows, filtered, selected, scroll, visibleRows,
                        filter.toString(), filterable, actions, marked);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> selected = Math.max(0, selected - 1);
                case DOWN -> selected = Math.min(filtered.size() - 1, selected + 1);
                case PAGE_UP -> selected = Math.max(0, selected - visibleRows);
                case PAGE_DOWN -> selected = Math.min(filtered.size() - 1, selected + visibleRows);
                case HOME -> selected = 0;
                case END -> selected = filtered.size() - 1;
                case ENTER -> {
                    if (!filtered.isEmpty()) {
                        return BrowseResult.select(filtered.get(selected));
                    }
                }
                case ESCAPE -> {
                    if (multi && !marked.isEmpty()) {
                        marked.clear();
                    } else if (filterable && filter.length() > 0) {
                        filter.setLength(0);
                        selected = 0;
                    } else {
                        return BrowseResult.back();
                    }
                }
                case BACKSPACE -> {
                    if (filterable && filter.length() > 0) {
                        filter.deleteCharAt(filter.length() - 1);
                        selected = 0;
                    }
                }
                case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> {
                    FunctionKey pressed = toFunctionKey(key);
                    if (actions.containsKey(pressed) && !filtered.isEmpty()) {
                        return BrowseResult.action(pressed, filtered.get(selected));
                    }
                }
                case CHARACTER -> {
                    Character ch = keys.lastCharacter();
                    if (multi && ch != null && ch == ' ') {
                        if (!filtered.isEmpty() && !marked.remove(filtered.get(selected))) {
                            marked.add(filtered.get(selected));
                        }
                    } else if (multi && ch != null && ch == CTRL_A) {
                        if (marked.containsAll(filtered)) {
                            marked.removeAll(filtered);
                        } else {
                            marked.addAll(filtered);
                        }
                    } else if (filterable) {
                        if (ch != null && !Character.isISOControl(ch)) {
                            filter.append(ch);
                            selected = 0;
                        }
                    } else if (keys.isQuitCharacter()) {
                        return BrowseResult.back();
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }
        } finally {
            if (multi) {
                state.setFilter(filter.toString());
                state.setRow(filtered.isEmpty() ? state.row() : filtered.get(selected));
            }
        }
    }

    @FunctionalInterface
    private interface BrowseContentProvider {
        BrowseContent get();
    }

    private record BrowseContent(List<String> header, List<String> rows) {
    }

    @Override
    public TableCellResult browseTableCells(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable, CellBrowseState state) throws Exception {
        Map<FunctionKey, String> actions = rowActions == null ? Map.of() : rowActions;
        int columnCount = table.columnCount();
        return withRawScreen(reader -> {
            StringBuilder filter = new StringBuilder(filterable ? state.filter() : "");
            int col = columnCount == 0 ? 0 : Math.min(state.col(), columnCount - 1);
            boolean cellMode = state.cellMode() && columnCount > 0;
            int targetRow = state.row();
            boolean restore = true;
            int selected = 0;
            int scroll = 0;
            int currentRow = targetRow;
            TableCellResult result = null;

            while (result == null) {
                int width = terminalWidth();
                TableLayout layout = table.layout(width);
                List<String> joined = joinRows(layout.rows());
                List<Integer> filtered = filterRows(joined, filter.toString());
                if (restore) {
                    selected = indexOfOriginal(filtered, targetRow);
                    restore = false;
                }
                if (selected >= filtered.size()) {
                    selected = Math.max(0, filtered.size() - 1);
                }
                if (col >= columnCount) {
                    col = Math.max(0, columnCount - 1);
                }
                currentRow = filtered.isEmpty() ? targetRow : filtered.get(selected);
                int reserved = 7 + (filterable ? 1 : 0) + (actions.isEmpty() ? 0 : 1);
                int visibleRows = visibleRows(reserved);
                scroll = clampScroll(selected, scroll, visibleRows);
                renderer.renderTableCells(title, layout, joined, filtered, selected, col, scroll, visibleRows,
                        filter.toString(), filterable, actions, cellMode);

                Key key = keys.readKey(reader);
                switch (key) {
                case UP -> selected = Math.max(0, selected - 1);
                case DOWN -> selected = Math.min(filtered.size() - 1, selected + 1);
                case PAGE_UP -> selected = Math.max(0, selected - visibleRows);
                case PAGE_DOWN -> selected = Math.min(filtered.size() - 1, selected + visibleRows);
                case HOME -> selected = 0;
                case END -> selected = filtered.size() - 1;
                case LEFT -> {
                    if (cellMode) {
                        col = Math.max(0, col - 1);
                    }
                }
                case RIGHT -> {
                    if (cellMode) {
                        col = Math.min(columnCount - 1, col + 1);
                    }
                }
                case TAB -> {
                    if (columnCount > 0) {
                        cellMode = !cellMode;
                    }
                }
                case ENTER -> {
                    if (!filtered.isEmpty()) {
                        int rowIndex = filtered.get(selected);
                        result = cellMode ? TableCellResult.selectCell(rowIndex, col)
                                : TableCellResult.selectRow(rowIndex);
                    }
                }
                case ESCAPE -> {
                    if (filterable && filter.length() > 0) {
                        filter.setLength(0);
                        selected = 0;
                    } else {
                        result = TableCellResult.back();
                    }
                }
                case BACKSPACE -> {
                    if (filterable && filter.length() > 0) {
                        filter.deleteCharAt(filter.length() - 1);
                        selected = 0;
                    }
                }
                case F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12 -> {
                    FunctionKey pressed = toFunctionKey(key);
                    if (actions.containsKey(pressed) && !filtered.isEmpty()) {
                        result = TableCellResult.action(pressed, filtered.get(selected), cellMode);
                    }
                }
                case CHARACTER -> {
                    Character ch = keys.lastCharacter();
                    if (filterable && ch != null && !Character.isISOControl(ch)) {
                        filter.append(ch);
                        selected = 0;
                    }
                }
                case INTERRUPT -> throw new UserInterruptException("");
                case EOF -> throw new EndOfFileException();
                default -> {
                }
                }
            }

            state.setRow(currentRow);
            state.setCol(col);
            state.setCellMode(cellMode);
            state.setFilter(filter.toString());
            return result;
        });
    }

    private static int indexOfOriginal(List<Integer> filtered, int originalRow) {
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i) == originalRow) {
                return i;
            }
        }
        return 0;
    }

    private static List<String> joinRows(List<List<String>> rowCells) {
        List<String> joined = new ArrayList<>(rowCells.size());
        for (List<String> cells : rowCells) {
            joined.add(String.join(Table.columnSeparator(), cells));
        }
        return joined;
    }

    private static List<Integer> filterRows(List<String> rows, String filter) {
        List<Integer> indices = new ArrayList<>();
        String needle = filter.toLowerCase(Locale.ROOT);
        for (int i = 0; i < rows.size(); i++) {
            if (needle.isEmpty() || rows.get(i).toLowerCase(Locale.ROOT).contains(needle)) {
                indices.add(i);
            }
        }
        return indices;
    }

    private int clampScroll(int selectedIndex, int scrollOffset, int visibleRows) {
        if (selectedIndex < scrollOffset) {
            return selectedIndex;
        }
        if (selectedIndex >= scrollOffset + visibleRows) {
            return selectedIndex - visibleRows + 1;
        }
        return scrollOffset;
    }

    private <T> T withRawScreen(ScreenAction<T> action) throws Exception {
        Attributes previous = terminal.enterRawMode();
        NonBlockingReader reader = terminal.reader();
        resizePending.set(false);
        Terminal.SignalHandler previousWinch =
                terminal.handle(Terminal.Signal.WINCH, signal -> resizePending.set(true));
        try {
            return action.run(reader);
        } finally {
            terminal.handle(Terminal.Signal.WINCH, previousWinch);
            renderer.clear();
            terminal.setAttributes(previous);
            terminal.writer().flush();
        }
    }

    private int visibleRows(int reservedLines) {
        int height = terminal.getHeight();
        if (height <= 0) {
            height = 24;
        }
        return Math.max(3, height - reservedLines);
    }

    @FunctionalInterface
    private interface ScreenAction<T> {
        T run(NonBlockingReader reader) throws Exception;
    }

    /** Current terminal width, falling back to {@link #DEFAULT_TERMINAL_WIDTH} when unknown. */
    private int terminalWidth() {
        int width = terminal.getWidth();
        return width <= 0 ? DEFAULT_TERMINAL_WIDTH : width;
    }

}
