package com.xsoftwareforge.xconsoleui.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.config.ListViewMode;
import com.xsoftwareforge.xconsoleui.config.MenuConfig;
import com.xsoftwareforge.xconsoleui.menu.AbstractMenu;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.Hotkey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.MenuRegistry;
import com.xsoftwareforge.xconsoleui.output.ConsoleOutputCoordinator;
import com.xsoftwareforge.xconsoleui.screen.ScreenChoice;
import com.xsoftwareforge.xconsoleui.screen.TerminalScreen;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableViewAction;

public class MenuSessionTest {

    @Test
    public void descendsIntoSubmenuRunsActionAndReturnsToRoot() throws Exception {
        List<String> log = new ArrayList<>();

        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                    log.add("action-ran");
                    ctx.print("done");
                }));
            }
        });

        // Root select -> open "alpha"; Alpha select -> run "act";
        // Alpha select -> Esc (back to root); Root select -> Esc (exit).
        ScriptedScreen screen = new ScriptedScreen("alpha", "act", null, null);

        List<String> sink = new ArrayList<>();
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(sink::add);

        MenuSession session = new MenuSession(screen, registry, coordinator, "Root");
        session.open();

        assertEquals(List.of("action-ran"), log);
        assertTrue("action output should be delivered", sink.contains("done"));
        // Two screens shown at root level (open + final esc) and two at alpha level.
        assertEquals(List.of("Root", "Alpha", "Alpha", "Root"), screen.titlesShown);
    }

    @Test
    public void rootEscapeExitsImmediately() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                }));
            }
        });

        ScriptedScreen screen = new ScriptedScreen((String) null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        MenuSession session = new MenuSession(screen, registry, coordinator, "Root");
        session.open();

        assertEquals(List.of("Root"), screen.titlesShown);
    }

    @Test
    public void globalHotkeyRunsItsAction() throws Exception {
        List<String> log = new ArrayList<>();

        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                }));
            }
        });
        registry.addHotkeyOption(HotkeyOption.of("global", "Global", FunctionKey.F2, ctx -> log.add("global-f2")));

        // Enter alpha, press F2 (global hotkey), Esc back, Esc exit.
        ScriptedScreen screen = new ScriptedScreen("alpha", "hk:F2", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        new MenuSession(screen, registry, coordinator, "Root").open();

        assertEquals(List.of("global-f2"), log);
    }

    @Test
    public void perMenuHotkeyOverridesGlobal() throws Exception {
        List<String> log = new ArrayList<>();

        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                }));
            }

            @Override
            public List<Hotkey> hotkeys() {
                return List.of(new Hotkey(FunctionKey.F2, "Local", ctx -> log.add("local-f2")));
            }
        });
        registry.addHotkeyOption(HotkeyOption.of("global", "Global", FunctionKey.F2, ctx -> log.add("global-f2")));

        ScriptedScreen screen = new ScriptedScreen("alpha", "hk:F2", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        new MenuSession(screen, registry, coordinator, "Root").open();

        assertEquals("per-menu hotkey must win over the global one", List.of("local-f2"), log);
    }

    @Test
    public void rebindingMovesGlobalHotkeyToNewKey() throws Exception {
        List<String> log = new ArrayList<>();

        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                }));
            }
        });
        registry.addHotkeyOption(HotkeyOption.of("help", "Help", FunctionKey.F2, ctx -> log.add("help")));

        // User reassigned the "help" option from its default F2 to F3.
        MenuConfig config = MenuConfig.defaults();
        config.assignHotkey("help", FunctionKey.F3);

        // Pressing F3 (the new key) must now trigger the hotkey; F2 would do nothing.
        ScriptedScreen screen = new ScriptedScreen("alpha", "hk:F3", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        new MenuSession(screen, registry, coordinator, "Root", config).open();

        assertEquals(List.of("help"), log);
    }

    @Test
    public void gridListViewUsesGridResultView() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Show", ctx -> ctx.showResult("Report", List.of("a", "b"))));
            }
        });

        ScriptedScreen screen = new ScriptedScreen("alpha", "act", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        MenuConfig config = MenuConfig.defaults();
        config.setListViewMode(ListViewMode.GRID);

        new MenuSession(screen, registry, coordinator, "Root", config).open();

        assertEquals(1, screen.showResultCalls);
        assertTrue("grid default should pass grid=true to showResult", screen.lastShowResultGrid);
    }

    @Test
    public void listModeUsesPlainResultViewAfterToggle() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Show", ctx -> ctx.showResult("Report", List.of("a", "b"))));
            }
        });

        ScriptedScreen screen = new ScriptedScreen("alpha", "act", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        MenuConfig config = MenuConfig.defaults(); // default LIST

        new MenuSession(screen, registry, coordinator, "Root", config).open();

        assertEquals(1, screen.showResultCalls);
        assertFalse("list default should pass grid=false to showResult", screen.lastShowResultGrid);
    }

    @Test
    public void toggleViewSwitchesOnlyTheCurrentMenuToGrid() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                }));
            }
        });

        // On the root menu: toggle (-> grid), then Esc.
        ScriptedScreen screen = new ScriptedScreen("toggle", null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        MenuConfig config = MenuConfig.defaults(); // default LIST
        new MenuSession(screen, registry, coordinator, "Root", config).open();

        // The first render was LIST (grid=false); after toggle the root re-rendered as grid.
        assertTrue("after toggle the menu must re-render in grid", screen.lastSelectGrid);
    }

    @Test
    public void failingActionIsReportedToErrorHandler() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        RuntimeException boom = new RuntimeException("boom");
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                    throw boom;
                }));
            }
        });

        ScriptedScreen screen = new ScriptedScreen("alpha", "act", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        List<Throwable> captured = new ArrayList<>();
        MenuSession session = new MenuSession(screen, registry, coordinator, "Root").onActionError(captured::add);
        session.open();

        // The failure is handed to the hook (with the original throwable) and the
        // session still returns to the root and exits normally.
        assertEquals(List.of(boom), captured);
    }

    @Test
    public void misbehavingErrorHandlerDoesNotBreakTheSession() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", ctx -> {
                    throw new RuntimeException("boom");
                }));
            }
        });

        ScriptedScreen screen = new ScriptedScreen("alpha", "act", null, null);
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });

        MenuSession session = new MenuSession(screen, registry, coordinator, "Root")
                .onActionError(ex -> {
                    throw new IllegalStateException("handler blew up");
                });
        // Must not propagate the handler's exception; the menu loop survives and exits.
        session.open();

        assertEquals(List.of("Root", "Alpha", "Alpha", "Root"), screen.titlesShown);
    }

    /** A {@link TerminalScreen} that replays a fixed sequence of selection results. */
    private static final class ScriptedScreen implements TerminalScreen {

        private static final String NULL_MARKER = " __null__";
        private static final String HOTKEY_PREFIX = "hk:";

        private final Deque<String> answers = new ArrayDeque<>();
        private final List<String> titlesShown = new ArrayList<>();
        private int showResultCalls;
        private boolean lastShowResultGrid;
        private boolean lastSelectGrid;

        ScriptedScreen(String... scripted) {
            for (String answer : scripted) {
                answers.addLast(answer == null ? NULL_MARKER : answer);
            }
        }

        @Override
        public ScreenChoice select(String title, List<MenuItem> options, Map<FunctionKey, String> hotkeys,
                boolean grid) {
            titlesShown.add(title);
            lastSelectGrid = grid;
            if (answers.isEmpty()) {
                return ScreenChoice.back();
            }
            String next = answers.pollFirst();
            if (NULL_MARKER.equals(next)) {
                return ScreenChoice.back();
            }
            if ("toggle".equals(next)) {
                return ScreenChoice.toggleView();
            }
            if (next.startsWith(HOTKEY_PREFIX)) {
                return ScreenChoice.hotkey(FunctionKey.valueOf(next.substring(HOTKEY_PREFIX.length())));
            }
            return ScreenChoice.item(next);
        }

        @Override
        public String select(String title, List<MenuItem> options) {
            ScreenChoice choice = select(title, options, Map.of(), false);
            return choice.isItem() ? choice.itemId() : null;
        }

        @Override
        public List<String> selectMultiple(String title, List<MenuItem> options, List<String> selectedIds) {
            return List.of();
        }

        @Override
        public String input(String title, String defaultValue, Character mask) {
            return defaultValue;
        }

        @Override
        public boolean confirm(String title, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public void showResult(String title, List<String> lines, boolean grid) {
            showResultCalls++;
            lastShowResultGrid = grid;
        }

        @Override
        public TableViewAction showTable(String title, Table table, boolean stickyHeader, boolean exportEnabled) {
            titlesShown.add(title);
            return TableViewAction.BACK;
        }

        @Override
        public BrowseResult browse(String title, List<String> header, List<String> rows,
                Map<FunctionKey, String> rowActions, boolean filterable) {
            titlesShown.add(title);
            return BrowseResult.back();
        }

        @Override
        public BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions,
                boolean filterable) {
            titlesShown.add(title);
            return BrowseResult.back();
        }

        @Override
        public com.xsoftwareforge.xconsoleui.menu.TableCellResult browseTableCells(String title, Table table,
                Map<FunctionKey, String> rowActions, boolean filterable,
                com.xsoftwareforge.xconsoleui.menu.CellBrowseState state) {
            titlesShown.add(title);
            return com.xsoftwareforge.xconsoleui.menu.TableCellResult.back();
        }
    }
}
