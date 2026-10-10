package com.xsoftwareforge.xconsoleui.runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.list.RowBrowser;
import com.xsoftwareforge.xconsoleui.list.TableBrowser;
import com.xsoftwareforge.xconsoleui.list.TableColumnDef;
import com.xsoftwareforge.xconsoleui.menu.AbstractMenu;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.CellBrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.Hotkey;
import com.xsoftwareforge.xconsoleui.menu.MenuAction;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.MenuRegistry;
import com.xsoftwareforge.xconsoleui.menu.RowActionEvent;
import com.xsoftwareforge.xconsoleui.menu.TableCellResult;
import com.xsoftwareforge.xconsoleui.output.ConsoleOutputCoordinator;
import com.xsoftwareforge.xconsoleui.screen.ScreenChoice;
import com.xsoftwareforge.xconsoleui.screen.TerminalScreen;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableViewAction;

public class MenuSessionObserverTest {

    @Test
    public void itemActionReportsLabelAndNull() throws Exception {
        List<String> log = new ArrayList<>();
        MenuSession session = session(new FakeScreen("alpha", "act"), ctx -> log.add("ran"))
                .onActionComplete((label, error) -> log.add(label + "|" + error));

        session.open();

        assertEquals(List.of("ran", "Do it|null"), log);
    }

    @Test
    public void hotkeyActionReportsItsHintLabel() throws Exception {
        List<String> log = new ArrayList<>();
        MenuSession session = session(new FakeScreen("alpha", "hk:F2"), ctx -> {
        }).onActionComplete((label, error) -> log.add(label + "|" + error));

        session.open();

        assertEquals(List.of("Do it|null"), log);
    }

    @Test
    public void failingActionReportsExceptionAndStillCallsErrorHandler() throws Exception {
        RuntimeException boom = new RuntimeException("boom");
        List<Throwable> reported = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        MenuSession session = session(new FakeScreen("alpha", "act"), ctx -> {
            throw boom;
        }).onActionComplete((label, error) -> reported.add(error)).onActionError(errors::add);

        session.open();

        assertEquals(List.of(boom), reported);
        assertEquals(List.of(boom), errors);
    }

    @Test
    public void throwingObserverDoesNotStopActionOrSession() throws Exception {
        List<String> log = new ArrayList<>();
        FakeScreen screen = new FakeScreen("alpha", "act", "act").inputs("Tom", "Tim");
        MenuSession session = session(screen, ctx -> log.add(ctx.input("Spieler")))
                .onActionComplete((label, error) -> {
                    throw new IllegalStateException("observer blew up");
                })
                .onInput((title, value) -> {
                    throw new IllegalStateException("observer blew up");
                });

        session.open();

        assertEquals("the action finishes and the menu keeps going", List.of("Tom", "Tim"), log);
    }

    @Test
    public void withoutObserversActionsRunAsBefore() throws Exception {
        List<String> log = new ArrayList<>();
        FakeScreen screen = new FakeScreen("alpha", "act").inputs("Tom").browse(BrowseResult.action(FunctionKey.F2, 0));
        session(screen, ctx -> {
            log.add(ctx.input("Spieler"));
            RowBrowser.create("Rows", List.of("a"), s -> s).action(FunctionKey.F2, "Kick", (c, row) -> log.add(row))
                    .open(ctx);
        }).open();

        assertEquals(List.of("Tom", "a"), log);
    }

    @Test
    public void inputReportsTitleAndValue() throws Exception {
        List<String> inputs = new ArrayList<>();
        MenuSession session = session(new FakeScreen("alpha", "act").inputs("Tom"), ctx -> ctx.input("Spieler"))
                .onInput((title, value) -> inputs.add(title + "=" + value));

        session.open();

        assertEquals(List.of("Spieler=Tom"), inputs);
    }

    @Test
    public void maskedInputIsReportedAsStarsOnly() throws Exception {
        List<String> inputs = new ArrayList<>();
        List<String> seenByAction = new ArrayList<>();
        MenuSession session = session(new FakeScreen("alpha", "act").inputs("geheim", null), ctx -> {
            seenByAction.add(ctx.inputMasked("Passwort"));
            seenByAction.add(ctx.inputMasked("Passwort"));
        }).onInput((title, value) -> inputs.add(title + "=" + value));

        session.open();

        assertEquals("the action still gets the plain text", "geheim", seenByAction.get(0));
        assertEquals("Esc stays null instead of ***", List.of("Passwort=***", "Passwort=null"), inputs);
    }

    @Test
    public void confirmAndSelectReportTheChosenOption() throws Exception {
        List<String> inputs = new ArrayList<>();
        // "alpha" and "act" drive the menu; "b" answers the select prompt inside the action.
        MenuSession session = session(new FakeScreen("alpha", "act", "b"), ctx -> {
            ctx.confirm("Sicher?", true);
            ctx.select("Wahl", List.of(MenuItem.action("a", "A", c -> {
            }), MenuItem.action("b", "B", c -> {
            })));
        }).onInput((title, value) -> inputs.add(title + "=" + value));

        session.open();

        assertEquals(List.of("Sicher?=true", "Wahl=b"), inputs);
    }

    @Test
    public void rowActionsAreReportedOneByOneWhileTheBrowserStaysOpen() throws Exception {
        List<String> log = new ArrayList<>();
        FakeScreen screen = new FakeScreen("alpha", "act")
                .browse(BrowseResult.action(FunctionKey.F2, 0), BrowseResult.action(FunctionKey.F3, 1));
        MenuSession session = session(screen, ctx -> RowBrowser.create("Players", List.of("Tom", "Ann"), s -> "#" + s)
                .action(FunctionKey.F2, "Faction", (c, row) -> log.add("run F2 " + row))
                .action(FunctionKey.F3, "Ban", (c, row) -> log.add("run F3 " + row))
                .open(ctx))
                .onRowAction(e -> log.add("event " + e.browserTitle() + "|" + e.row() + "|" + e.actionKey() + "|"
                        + e.actionLabel() + "|" + e.error()))
                .onActionComplete((label, error) -> log.add("done " + label));

        session.open();

        assertEquals(List.of(
                "run F2 Tom", "event Players|#Tom|F2|Faction|null",
                "run F3 Ann", "event Players|#Ann|F3|Ban|null",
                "done Do it"), log);
    }

    @Test
    public void failingRowActionIsReportedWithItsError() throws Exception {
        RuntimeException boom = new RuntimeException("boom");
        List<RowActionEvent> events = new ArrayList<>();
        List<Throwable> completed = new ArrayList<>();
        FakeScreen screen = new FakeScreen("alpha", "act").browse(BrowseResult.action(FunctionKey.F2, 0));
        MenuSession session = session(screen, ctx -> RowBrowser.create("Rows", List.of("a"), s -> s)
                .action(FunctionKey.F2, "Boom", (c, row) -> {
                    throw boom;
                }).open(ctx))
                .onRowAction(events::add)
                .onActionComplete((label, error) -> completed.add(error));

        session.open();

        assertEquals(1, events.size());
        assertSame(boom, events.get(0).error());
        assertEquals("the failure still reaches the surrounding action", List.of(boom), completed);
    }

    @Test
    public void tableBrowserReportsFirstCellForActionsAndCellEdits() throws Exception {
        List<String> log = new ArrayList<>();
        FakeScreen screen = new FakeScreen("alpha", "act").inputs("42")
                .cells(TableCellResult.action(FunctionKey.F2, 0, false), TableCellResult.selectCell(0, 1));
        MenuSession session = session(screen, ctx -> TableBrowser.create("Planets", List.<String[]>of(new String[] {"Earth", "5"}))
                .column(TableColumnDef.of("Name", r -> r[0]))
                .column(TableColumnDef.<String[]>of("Pop", r -> r[1]).editable((c, r) -> c.input("Pop")))
                .action(FunctionKey.F2, "Rename", (c, r) -> {
                })
                .open(ctx))
                .onRowAction(e -> log.add(e.row() + "|" + e.actionKey() + "|" + e.actionLabel()))
                .onInput((title, value) -> log.add(title + "=" + value));

        session.open();

        assertEquals(List.of("Earth|F2|Rename", "Pop=42", "Earth|null|Edit Pop"), log);
    }

    @Test
    public void selectHandlerIsReportedWithoutKey() throws Exception {
        List<RowActionEvent> events = new ArrayList<>();
        FakeScreen screen = new FakeScreen("alpha", "act").browse(BrowseResult.select(0));
        MenuSession session = session(screen, ctx -> RowBrowser.create("Rows", List.of("a"), s -> s)
                .onSelect((c, row) -> {
                }).open(ctx))
                .onRowAction(events::add);

        session.open();

        assertEquals(List.of(new RowActionEvent("Rows", "a", null, "Select", null)), events);
        assertNull(events.get(0).actionKey());
    }

    /** A session over one menu "alpha" with item "act" ("Do it") and the same action on F2. */
    private static MenuSession session(FakeScreen screen, MenuAction action) {
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(new AbstractMenu("alpha", "Alpha") {
            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(MenuItem.action("act", "Do it", action));
            }

            @Override
            public List<Hotkey> hotkeys() {
                return List.of(new Hotkey(FunctionKey.F2, "Do it", action));
            }
        });
        ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
        coordinator.attachPrinter(line -> {
        });
        return new MenuSession(screen, registry, coordinator, "Root");
    }

    /** Replays scripted menu choices, inputs and browser results; Esc once a script runs dry. */
    private static final class FakeScreen implements TerminalScreen {

        private static final String HOTKEY_PREFIX = "hk:";

        private final Deque<String> choices = new ArrayDeque<>();
        private final Deque<String> inputs = new ArrayDeque<>();
        private final Deque<BrowseResult> browses = new ArrayDeque<>();
        private final Deque<TableCellResult> cells = new ArrayDeque<>();

        FakeScreen(String... scripted) {
            choices.addAll(List.of(scripted));
        }

        FakeScreen inputs(String... values) {
            for (String value : values) {
                inputs.addLast(value == null ? "\0" : value);
            }
            return this;
        }

        FakeScreen browse(BrowseResult... results) {
            browses.addAll(List.of(results));
            return this;
        }

        FakeScreen cells(TableCellResult... results) {
            cells.addAll(List.of(results));
            return this;
        }

        @Override
        public ScreenChoice select(String title, List<MenuItem> options, Map<FunctionKey, String> hotkeys,
                boolean grid) {
            String next = choices.pollFirst();
            if (next == null) {
                return ScreenChoice.back();
            }
            if (next.startsWith(HOTKEY_PREFIX)) {
                return ScreenChoice.hotkey(FunctionKey.valueOf(next.substring(HOTKEY_PREFIX.length())));
            }
            return ScreenChoice.item(next);
        }

        @Override
        public String select(String title, List<MenuItem> options) {
            return choices.pollFirst();
        }

        @Override
        public List<String> selectMultiple(String title, List<MenuItem> options, List<String> selectedIds) {
            return List.of();
        }

        @Override
        public String input(String title, String defaultValue, Character mask) {
            String next = inputs.pollFirst();
            return next == null ? defaultValue : "\0".equals(next) ? null : next;
        }

        @Override
        public boolean confirm(String title, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public void showResult(String title, List<String> lines, boolean grid) {
        }

        @Override
        public TableViewAction showTable(String title, Table table, boolean stickyHeader, boolean exportEnabled) {
            return TableViewAction.BACK;
        }

        @Override
        public BrowseResult browse(String title, List<String> header, List<String> rows,
                Map<FunctionKey, String> rowActions, boolean filterable) {
            BrowseResult next = browses.pollFirst();
            return next == null ? BrowseResult.back() : next;
        }

        @Override
        public BrowseResult browseTable(String title, Table table, Map<FunctionKey, String> rowActions,
                boolean filterable) {
            BrowseResult next = browses.pollFirst();
            return next == null ? BrowseResult.back() : next;
        }

        @Override
        public TableCellResult browseTableCells(String title, Table table, Map<FunctionKey, String> rowActions,
                boolean filterable, CellBrowseState state) {
            TableCellResult next = cells.pollFirst();
            return next == null ? TableCellResult.back() : next;
        }
    }
}
