package com.xsoftwareforge.xconsoleui.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.form.StubMenuContext;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.RowActionEvent;

public class RowBrowserMultiSelectTest {

    private final List<List<String>> batches = new ArrayList<>();

    private RowBrowser<String> browser(List<String> rows) {
        return RowBrowser.create("Asteroids", () -> rows, s -> s)
                .multiSelect(true)
                .batchAction(FunctionKey.F5, "Collect", (ctx, items) -> batches.add(items));
    }

    @Test
    public void batchActionReceivesMarkedRowsInListOrder() throws Exception {
        StubMenuContext ctx = new StubMenuContext()
                .queueMarked(2, 0).queueBrowse(BrowseResult.action(FunctionKey.F5, 1));

        browser(List.of("a", "b", "c", "d")).open(ctx);

        assertEquals(List.of(List.of("a", "c")), batches);
        assertEquals("marks cleared after the batch ran", Set.of(), ctx.marksSeen.get(1));
    }

    @Test
    public void batchActionWithoutMarksReceivesCursorRow() throws Exception {
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.action(FunctionKey.F5, 1));

        browser(List.of("a", "b", "c")).open(ctx);

        assertEquals(List.of(List.of("b")), batches);
    }

    @Test
    public void batchActionOnPlainBrowserReceivesCursorRow() throws Exception {
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.action(FunctionKey.F5, 2));

        RowBrowser.create("Plain", List.of("a", "b", "c"), s -> s)
                .batchAction(FunctionKey.F5, "Collect", (c, items) -> batches.add(items))
                .open(ctx);

        assertEquals(List.of(List.of("c")), batches);
        assertTrue("plain browser never uses the stateful browse", ctx.marksSeen.isEmpty());
    }

    @Test
    public void marksFollowTheirRowsWhenTheSourceChanges() throws Exception {
        List<String> rows = new ArrayList<>(List.of("a", "b", "c"));
        StubMenuContext ctx = new StubMenuContext()
                .queueMarked(1, 2).queueBrowse(BrowseResult.action(FunctionKey.F2, 0));

        RowBrowser.create("Live", () -> rows, s -> s)
                .multiSelect(true)
                .action(FunctionKey.F2, "Remove", (c, item) -> rows.remove(item))
                .open(ctx);

        // "a" removed: b and c are now rows 0 and 1, and still marked.
        assertEquals(Set.of(0, 1), ctx.marksSeen.get(1));
    }

    @Test
    public void rowBatchConfirmsOnceRunsEachItemAndSummarises() throws Exception {
        List<String> done = new ArrayList<>();
        StubMenuContext ctx = new StubMenuContext().queueConfirm(true)
                .queueMarked(0, 1, 2).queueBrowse(BrowseResult.action(FunctionKey.F5, 0));

        RowBrowser.create("Asteroids", List.of("a1", "a2", "a3"), s -> s)
                .multiSelect(true)
                .batchAction(FunctionKey.F5, "Delete", (c, items) -> RowBatch.run(c, "Delete", items, s -> s,
                        (c2, item) -> {
                            if (item.equals("a2")) {
                                throw new RowBatch.Skip("besetzt");
                            }
                            if (item.equals("a3")) {
                                throw new IllegalStateException("kaputt");
                            }
                            done.add(item);
                        }))
                .open(ctx);

        assertEquals(List.of("a1"), done);
        assertEquals(List.of("Delete: Für 3 Einträge ausführen?"), ctx.confirmTitles);
        assertEquals(List.of("Delete"), ctx.resultTitles);
        assertEquals(List.of(
                "1 ok, 1 übersprungen, 1 fehlgeschlagen",
                "",
                "OK            a1",
                "ÜBERSPRUNGEN  a2: besetzt",
                "FEHLER        a3: kaputt"), ctx.resultLines.get(0));

        // Audit: one event per item with its own outcome, not one for the batch.
        assertEquals(3, ctx.rowEvents.size());
        RowActionEvent ok = ctx.rowEvents.get(0);
        assertEquals("Asteroids", ok.browserTitle());
        assertEquals("a1", ok.row());
        assertEquals(FunctionKey.F5, ok.actionKey());
        assertEquals("Delete", ok.actionLabel());
        assertNull(ok.error());
        assertTrue(ctx.rowEvents.get(1).error() instanceof RowBatch.Skip);
        assertTrue(ctx.rowEvents.get(2).error() instanceof IllegalStateException);
    }

    @Test
    public void declinedConfirmationRunsNothingAndKeepsMarks() throws Exception {
        List<String> done = new ArrayList<>();
        StubMenuContext ctx = new StubMenuContext().queueConfirm(false)
                .queueMarked(0, 1).queueBrowse(BrowseResult.action(FunctionKey.F5, 0));

        RowBrowser.create("Asteroids", List.of("a1", "a2"), s -> s)
                .multiSelect(true)
                .batchAction(FunctionKey.F5, "Delete",
                        (c, items) -> RowBatch.run(c, "Delete", items, s -> s, (c2, item) -> done.add(item)))
                .open(ctx);

        assertTrue(done.isEmpty());
        assertTrue(ctx.resultLines.isEmpty());
        assertTrue(ctx.rowEvents.isEmpty());
        assertEquals(Set.of(0, 1), ctx.marksSeen.get(1));
    }

    @Test
    public void singleItemRowBatchSkipsConfirmation() throws Exception {
        List<String> done = new ArrayList<>();
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.action(FunctionKey.F5, 1));

        RowBrowser.create("Asteroids", List.of("a1", "a2"), s -> s)
                .multiSelect(true)
                .batchAction(FunctionKey.F5, "Delete",
                        (c, items) -> RowBatch.run(c, "Delete", items, s -> s, (c2, item) -> done.add(item)))
                .open(ctx);

        assertEquals(List.of("a2"), done);
        assertTrue(ctx.confirmTitles.isEmpty());
        assertEquals("1 ok, 0 übersprungen, 0 fehlgeschlagen", ctx.resultLines.get(0).get(0));
    }

    @Test
    public void plainBatchHandlerIsReportedOncePerItem() throws Exception {
        StubMenuContext ctx = new StubMenuContext()
                .queueMarked(0, 2).queueBrowse(BrowseResult.action(FunctionKey.F5, 0));

        browser(List.of("a", "b", "c")).open(ctx);

        assertEquals(2, ctx.rowEvents.size());
        assertEquals("a", ctx.rowEvents.get(0).row());
        assertEquals("c", ctx.rowEvents.get(1).row());
        assertEquals("Collect", ctx.rowEvents.get(1).actionLabel());
    }

    @Test
    public void singleRowActionsIgnoreMarks() throws Exception {
        List<String> hit = new ArrayList<>();
        StubMenuContext ctx = new StubMenuContext()
                .queueMarked(0, 1).queueBrowse(BrowseResult.action(FunctionKey.F2, 2));

        RowBrowser.create("Asteroids", List.of("a", "b", "c"), s -> s)
                .multiSelect(true)
                .action(FunctionKey.F2, "One", (c, item) -> hit.add(item))
                .open(ctx);

        assertEquals(List.of("c"), hit);
        assertEquals(Set.of(0, 1), ctx.marksSeen.get(1));
    }

    private record Rock(String name, int size) {
    }

    @Test
    public void tableBrowserBatchActionReceivesMarkedRows() throws Exception {
        List<List<Rock>> seen = new ArrayList<>();
        StubMenuContext ctx = new StubMenuContext()
                .queueMarked(0, 2).queueBrowse(BrowseResult.action(FunctionKey.F5, 1));

        TableBrowser.create("Rocks", List.of(new Rock("r1", 1), new Rock("r2", 2), new Rock("r3", 3)))
                .column(TableColumnDef.of("Name", Rock::name))
                .multiSelect(true)
                .batchAction(FunctionKey.F5, "Collect", (c, items) -> seen.add(items))
                .open(ctx);

        assertEquals(List.of(List.of(new Rock("r1", 1), new Rock("r3", 3))), seen);
        assertEquals("r3", ctx.rowEvents.get(1).row());
    }
}
