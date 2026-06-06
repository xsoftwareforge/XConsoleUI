package com.xsoftwareforge.xconsoleui.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.form.StubMenuContext;
import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.table.Align;

public class TableBrowserTest {

    private record Planet(String name, int pop) {
    }

    @Test
    public void runsRowActionOnSelectedTableRow() throws Exception {
        List<Planet> planets = new ArrayList<>(List.of(
                new Planet("Earth", 8000), new Planet("Mars", 120), new Planet("Io", 3)));
        List<String> deleted = new ArrayList<>();

        TableBrowser<Planet> browser = TableBrowser.create("Planets", () -> planets)
                .column(TableColumnDef.of("Name", Planet::name))
                .column(TableColumnDef.of("Pop", Align.RIGHT, p -> String.valueOf(p.pop())))
                .action(FunctionKey.F2, "Delete", (ctx, p) -> {
                    deleted.add(p.name());
                    planets.remove(p);
                });

        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.action(FunctionKey.F2, 2));

        browser.open(ctx);

        assertEquals(List.of("Io"), deleted);
        assertEquals(2, planets.size());
    }

    @Test
    public void editsEditableCellInCellMode() throws Exception {
        List<int[]> box = new ArrayList<>();
        box.add(new int[] {5}); // a mutable "row" holding one number

        TableBrowser<int[]> browser = TableBrowser.create("Nums", box)
                .column(TableColumnDef.<int[]>of("Value", r -> String.valueOf(r[0]))
                        .editable((ctx, r) -> {
                            String v = ctx.input("Value", String.valueOf(r[0]));
                            if (v != null) {
                                r[0] = Integer.parseInt(v);
                            }
                        }));

        // Select cell (row 0, col 0); the editor reads "42" from the stub input.
        StubMenuContext ctx = new StubMenuContext()
                .queueCell(com.xsoftwareforge.xconsoleui.menu.TableCellResult.selectCell(0, 0))
                .queueInput("42");

        browser.open(ctx);

        assertEquals(42, box.get(0)[0]);
    }

    @Test
    public void pickReturnsSelectedRow() throws Exception {
        List<Planet> planets = List.of(new Planet("Earth", 8000), new Planet("Mars", 120));
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.select(1));

        Optional<Planet> picked = TableBrowser.create("Planets", planets)
                .column(TableColumnDef.of("Name", Planet::name))
                .pick(ctx);

        assertTrue(picked.isPresent());
        assertEquals("Mars", picked.get().name());
    }
}
