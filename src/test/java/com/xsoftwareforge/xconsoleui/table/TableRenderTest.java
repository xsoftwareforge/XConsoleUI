package com.xsoftwareforge.xconsoleui.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableRenderTest {

    @Test
    public void columnWidthFitsHeaderAndContent() {
        Table table = TableBuilder.create()
                .column("Planet")
                .column("Pop", Align.RIGHT)
                .row("Earth", "8000")
                .row("A very long planet name", "1")
                .build();

        RenderedTable rendered = table.render(120);

        // Header and all rows share the same width.
        int width = rendered.header().length();
        assertEquals(width, rendered.separator().length());
        for (String row : rendered.rows()) {
            assertEquals(width, row.length());
        }
        // First column widened to the longest cell; right-aligned Pop column.
        assertTrue(rendered.header().startsWith("Planet"));
        assertTrue(rendered.rows().get(0).contains("8000"));
        assertTrue("right-aligned value should be padded on the left",
                rendered.rows().get(1).contains("  1") || rendered.rows().get(1).endsWith("1"));
    }

    @Test
    public void truncatesWithEllipsisWhenTooWide() {
        Table table = TableBuilder.create()
                .column("Name")
                .row("ThisIsAReallyLongValueThatWontFit")
                .build();

        RenderedTable rendered = table.render(10);

        assertTrue("line must fit the requested width", rendered.header().length() <= 10);
        assertTrue("overflowing cell should be truncated with an ellipsis",
                rendered.rows().get(0).contains("…"));
    }

    @Test
    public void emptyTableRendersEmptyLines() {
        Table table = TableBuilder.create().build();
        RenderedTable rendered = table.render(80);
        assertEquals("", rendered.header());
        assertTrue(rendered.rows().isEmpty());
    }
}
