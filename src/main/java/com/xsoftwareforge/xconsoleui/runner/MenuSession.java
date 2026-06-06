package com.xsoftwareforge.xconsoleui.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

import com.xsoftwareforge.xconsoleui.config.ListViewMode;
import com.xsoftwareforge.xconsoleui.config.MenuConfig;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.CellBrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.Hotkey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;
import com.xsoftwareforge.xconsoleui.menu.Menu;
import com.xsoftwareforge.xconsoleui.menu.MenuAction;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.MenuRegistry;
import com.xsoftwareforge.xconsoleui.menu.TableCellResult;
import com.xsoftwareforge.xconsoleui.output.ConsoleOutputCoordinator;
import com.xsoftwareforge.xconsoleui.output.SessionState;
import com.xsoftwareforge.xconsoleui.screen.JLineTerminalScreen;
import com.xsoftwareforge.xconsoleui.screen.ScreenChoice;
import com.xsoftwareforge.xconsoleui.screen.TerminalScreen;
import com.xsoftwareforge.xconsoleui.table.ExportFormat;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableExporter;
import com.xsoftwareforge.xconsoleui.table.TableViewAction;

/**
 * Drives navigation over a {@link MenuRegistry}. A session renders a synthetic
 * root menu listing every registered top-level menu, lets the user descend into
 * sub-menus and run leaf actions, and routes all interactive screens through the
 * {@link ConsoleOutputCoordinator} so asynchronous log output is buffered while a
 * screen is visible and replayed when it closes.
 * <p>
 * The session is the {@link MenuContext} passed to actions, so an action can
 * itself prompt, confirm or show results with the same buffering guarantees.
 */
public final class MenuSession implements MenuContext {

    private static final String SEPARATOR = "--------------------------------------------------";

    private final TerminalScreen screen;
    private final ConsoleOutputCoordinator coordinator;
    private final MenuRegistry registry;
    private final MenuConfig config;
    private final String rootTitle;
    private final Map<String, ListViewMode> menuViewModes = new HashMap<>();

    /** Receives the full throwable when an action fails; defaults to a no-op. */
    private volatile Consumer<Throwable> errorHandler = ex -> {
    };

    private volatile boolean running;

    public MenuSession(Terminal terminal, MenuRegistry registry, ConsoleOutputCoordinator coordinator,
            String rootTitle) {
        this(terminal, registry, coordinator, rootTitle, MenuConfig.defaults());
    }

    public MenuSession(Terminal terminal, MenuRegistry registry, ConsoleOutputCoordinator coordinator,
            String rootTitle, MenuConfig config) {
        this(new JLineTerminalScreen(terminal, config), registry, coordinator, rootTitle, config);
    }

    public MenuSession(TerminalScreen screen, MenuRegistry registry, ConsoleOutputCoordinator coordinator,
            String rootTitle) {
        this(screen, registry, coordinator, rootTitle, MenuConfig.defaults());
    }

    public MenuSession(TerminalScreen screen, MenuRegistry registry, ConsoleOutputCoordinator coordinator,
            String rootTitle, MenuConfig config) {
        this.screen = Objects.requireNonNull(screen, "screen");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.config = Objects.requireNonNull(config, "config");
        this.rootTitle = rootTitle == null ? "Menu" : rootTitle;
    }

    /**
     * Register a handler that receives the full {@link Throwable} (with stack trace)
     * whenever a menu action fails. The user still sees a concise error message; this
     * hook lets a host log or report the failure for debugging. Passing {@code null}
     * restores the default no-op handler.
     *
     * @return this session, for chaining
     */
    public MenuSession onActionError(Consumer<Throwable> handler) {
        this.errorHandler = handler == null ? ex -> {
        } : handler;
        return this;
    }

    /**
     * Open the menu and block until the user leaves the root level (Esc), the
     * session is {@link #stop() stopped}, or the terminal signals interrupt/EOF.
     */
    public void open() throws Exception {
        running = true;
        try {
            openMenu(rootMenu());
        } catch (UserInterruptException | EndOfFileException ex) {
            // Ctrl-C / Ctrl-D close the menu gracefully.
        } finally {
            running = false;
        }
    }

    /** Request the session to stop after the current screen returns. */
    public void stop() {
        running = false;
    }

    private void openMenu(Menu menu) throws Exception {
        while (running) {
            List<MenuItem> items = menu.items(this);
            if (items == null) {
                items = List.of();
            }
            Map<FunctionKey, String> hints = new LinkedHashMap<>();
            Map<FunctionKey, MenuAction> actions = new LinkedHashMap<>();
            collectHotkeys(menu, hints, actions);

            List<MenuItem> renderItems = items;
            boolean grid = menuViewModes.getOrDefault(menu.id(), config.defaultListViewMode()) == ListViewMode.GRID;
            ScreenChoice choice = runInteraction(SessionState.MENU,
                    () -> screen.select(menu.title(), renderItems, hints, grid));

            if (choice.isToggleView()) {
                menuViewModes.put(menu.id(), grid ? ListViewMode.LIST : ListViewMode.GRID);
                continue; // re-render this menu only, in the new layout
            }
            if (choice.isBack()) {
                return; // Esc → one level up
            }
            if (choice.isHotkey()) {
                MenuAction action = actions.get(choice.hotkey());
                if (action != null) {
                    runAction(action);
                }
                continue;
            }

            MenuItem chosen = findById(items, choice.itemId());
            if (chosen == null) {
                continue;
            }
            if (chosen.isSubmenu()) {
                openMenu(chosen.submenu());
            } else if (chosen.action() != null) {
                runAction(chosen.action());
            }
        }
    }

    /**
     * Resolve the hotkeys active for {@code menu}: global options (with the user's
     * key assignments from {@link MenuConfig} applied, skipping unbound ones)
     * overlaid with the menu's own per-menu hotkeys (menu wins per effective key).
     * Fills the {@code hints} (key → label) and {@code actions} (key → action) maps.
     */
    private void collectHotkeys(Menu menu, Map<FunctionKey, String> hints, Map<FunctionKey, MenuAction> actions) {
        for (HotkeyOption option : registry.hotkeyOptions()) {
            FunctionKey key = config.effectiveKey(option);
            if (key != null) {
                hints.put(key, option.label());
                actions.put(key, option.action());
            }
        }
        for (Hotkey hotkey : menu.hotkeys()) {
            hints.put(hotkey.key(), hotkey.label());
            actions.put(hotkey.key(), hotkey.action());
        }
    }

    private void runAction(com.xsoftwareforge.xconsoleui.menu.MenuAction action) throws Exception {
        try {
            action.run(this);
        } catch (UserInterruptException | EndOfFileException ex) {
            throw ex;
        } catch (Exception ex) {
            // Show a concise message to the user, but hand the full throwable (with
            // stack trace) to the host's error handler so failures can be logged.
            printError(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            notifyErrorHandler(ex);
        }
    }

    private void notifyErrorHandler(Exception actionFailure) {
        try {
            errorHandler.accept(actionFailure);
        } catch (RuntimeException handlerFailure) {
            // A misbehaving error hook must never tear down the menu loop; the action
            // failure was already reported to the user, so swallow the hook's error.
            actionFailure.addSuppressed(handlerFailure);
        }
    }

    private Menu rootMenu() {
        return new Menu() {
            @Override
            public String id() {
                return "__root__";
            }

            @Override
            public String title() {
                return rootTitle;
            }

            @Override
            public List<MenuItem> items(MenuContext context) {
                List<MenuItem> items = new ArrayList<>();
                for (Menu menu : registry.menus()) {
                    items.add(MenuItem.submenu(menu.id(), menu.title(), menu));
                }
                return items;
            }
        };
    }

    private static MenuItem findById(List<MenuItem> items, String id) {
        for (MenuItem item : items) {
            if (item.id().equals(id)) {
                return item;
            }
        }
        return null;
    }

    // ---- MenuContext: output -------------------------------------------------

    @Override
    public void print(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        coordinator.publish(message);
    }

    @Override
    public void printLines(List<String> lines) {
        if (lines == null) {
            return;
        }
        for (String line : lines) {
            print(line);
        }
    }

    @Override
    public void printSeparator() {
        print(SEPARATOR);
    }

    @Override
    public void printError(String message) {
        print("Error: " + message);
    }

    @Override
    public void printSuccess(String message) {
        print("Success: " + message);
    }

    // ---- MenuContext: interactive primitives --------------------------------

    @Override
    public String input(String title) throws Exception {
        return input(title, null);
    }

    @Override
    public String input(String title, String defaultValue) throws Exception {
        return runInteraction(SessionState.WIZARD, () -> screen.input(title, defaultValue, null));
    }

    @Override
    public String inputMasked(String title) throws Exception {
        return runInteraction(SessionState.WIZARD, () -> screen.input(title, null, '*'));
    }

    @Override
    public boolean confirm(String title) throws Exception {
        return confirm(title, false);
    }

    @Override
    public boolean confirm(String title, boolean defaultValue) throws Exception {
        return runInteraction(SessionState.WIZARD, () -> screen.confirm(title, defaultValue));
    }

    @Override
    public String select(String title, List<MenuItem> options) throws Exception {
        // Empty lists are handled by the screen (placeholder), e.g. an empty root menu.
        return runInteraction(SessionState.MENU, () -> screen.select(title, options));
    }

    @Override
    public List<String> selectMultiple(String title, List<MenuItem> options, List<String> preselected)
            throws Exception {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        return runInteraction(SessionState.MENU, () -> screen.selectMultiple(title, options, preselected));
    }

    @Override
    public void showResult(String title, List<String> lines) throws Exception {
        boolean grid = config.defaultListViewMode() == ListViewMode.GRID;
        runInteraction(SessionState.MENU, () -> {
            screen.showResult(title, lines, grid);
            return null;
        });
    }

    @Override
    public BrowseResult browse(String title, List<String> rows, Map<FunctionKey, String> rowActions,
            boolean filterable) throws Exception {
        Map<FunctionKey, String> actions = rowActions == null ? Map.of() : rowActions;
        return runInteraction(SessionState.MENU, () -> screen.browse(title, null, rows, actions, filterable));
    }

    @Override
    public BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable) throws Exception {
        Map<FunctionKey, String> actions = rowActions == null ? Map.of() : rowActions;
        return runInteraction(SessionState.MENU, () -> screen.browseTable(title, table, actions, filterable));
    }

    @Override
    public TableCellResult browseTableCells(String title, Table table, Map<FunctionKey, String> rowActions,
            boolean filterable, CellBrowseState state) throws Exception {
        Map<FunctionKey, String> actions = rowActions == null ? Map.of() : rowActions;
        return runInteraction(SessionState.MENU,
                () -> screen.browseTableCells(title, table, actions, filterable, state));
    }

    @Override
    public void notify(String title, List<String> lines) throws Exception {
        List<String> safe = lines == null ? List.of() : lines;
        runInteraction(SessionState.MENU, () -> {
            screen.showResult(title, safe, false);
            return null;
        });
    }

    @Override
    public void showConsole() throws Exception {
        List<String> history = coordinator.history();
        List<String> lines = history.isEmpty() ? List.of("(console is empty)") : history;
        runInteraction(SessionState.MENU, () -> {
            screen.showResult("Console", lines, false);
            return null;
        });
    }

    @Override
    public void showTable(String title, Table table) throws Exception {
        showTable(title, table, null);
    }

    @Override
    public void showTable(String title, Table table, TableExporter exporter) throws Exception {
        boolean sticky = config.tableHeaderSticky();
        boolean exportEnabled = exporter != null;
        while (running) {
            TableViewAction action = runInteraction(SessionState.MENU,
                    () -> screen.showTable(title, table, sticky, exportEnabled));
            if (action == TableViewAction.EXPORT && exporter != null) {
                runTableExport(title, table, exporter);
                continue;
            }
            return;
        }
    }

    private void runTableExport(String title, Table table, TableExporter exporter) throws Exception {
        String format = select("Export format", List.of(
                MenuItem.action("csv", "CSV", x -> {
                }),
                MenuItem.action("json", "JSON", x -> {
                }),
                MenuItem.action("both", "CSV + JSON", x -> {
                })));
        if (format == null) {
            return;
        }
        Set<ExportFormat> formats = switch (format) {
        case "csv" -> EnumSet.of(ExportFormat.CSV);
        case "json" -> EnumSet.of(ExportFormat.JSON);
        default -> EnumSet.of(ExportFormat.CSV, ExportFormat.JSON);
        };

        String name = input("File name (without extension)", slug(title));
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            List<Path> written = exporter.export(table, name, formats);
            List<String> lines = new ArrayList<>();
            lines.add("Saved " + written.size() + " file(s):");
            for (Path path : written) {
                lines.add("  " + path.toAbsolutePath());
            }
            showResult("Export complete", lines);
        } catch (IOException ex) {
            showResult("Export failed", List.of(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }

    private static String slug(String title) {
        if (title == null || title.isBlank()) {
            return "table";
        }
        return title.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void close() {
        running = false;
    }

    private <T> T runInteraction(SessionState state, ThrowingSupplier<T> supplier) throws Exception {
        coordinator.enterInteraction(state);
        try {
            return supplier.get();
        } finally {
            coordinator.leaveInteraction(SessionState.IDLE);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
