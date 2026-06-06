package com.xsoftwareforge.xconsoleui.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.form.StubMenuContext;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;

public class RowBrowserTest {

    @Test
    public void runsRowActionForHighlightedRowThenBack() throws Exception {
        List<String> sockets = new ArrayList<>(List.of("alice", "bob", "carol"));
        List<String> kicked = new ArrayList<>();

        RowBrowser<String> browser = RowBrowser.create("Sockets", () -> sockets, s -> s)
                .action(FunctionKey.F2, "Kick", (ctx, item) -> {
                    kicked.add(item);
                    sockets.remove(item);
                });

        // Press F2 on row index 1 (bob), then Esc (empty queue -> back()).
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.action(FunctionKey.F2, 1));

        browser.open(ctx);

        assertEquals(List.of("bob"), kicked);
        assertEquals(List.of("alice", "carol"), sockets);
    }

    @Test
    public void pickReturnsSelectedItem() throws Exception {
        List<String> items = List.of("x", "y", "z");
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.select(2));

        Optional<String> picked = RowBrowser.create("Pick", items, s -> s).pick(ctx);

        assertTrue(picked.isPresent());
        assertEquals("z", picked.get());
    }

    @Test
    public void pickReturnsEmptyOnBack() throws Exception {
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.back());
        assertFalse(RowBrowser.create("Pick", List.of("a"), s -> s).pick(ctx).isPresent());
    }
}
