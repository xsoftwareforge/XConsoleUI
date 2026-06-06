package com.xsoftwareforge.xconsoleui.screen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GridLayoutTest {

    @Test
    public void shortItemsAreArrangedIntoMultipleColumns() {
        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            items.add(String.format("item-%03d", i)); // 8 chars
        }

        List<String> lines = JLineTerminalScreen.composeGrid(items, 80);

        // 8-char items + 2 gap -> 8 columns at width 80 -> ceil(120/8) = 15 rows.
        assertEquals(15, lines.size());
        // First display line contains several items side by side (column-major: 1, 16, 31...).
        assertTrue(lines.get(0).contains("item-001"));
        assertTrue(lines.get(0).contains("item-016"));
        assertTrue("a grid line should be much wider than one item", lines.get(0).length() > 40);
    }

    @Test
    public void longItemsFallBackToSingleColumn() {
        List<String> items = List.of(
                "a very long descriptive line that will not fit twice",
                "another very long descriptive line of similar length");

        List<String> lines = JLineTerminalScreen.composeGrid(items, 80);

        assertEquals(2, lines.size()); // one per line
    }
}
