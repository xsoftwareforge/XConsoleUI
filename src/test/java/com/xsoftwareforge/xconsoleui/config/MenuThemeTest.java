package com.xsoftwareforge.xconsoleui.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.config.MenuTheme.Slot;

public class MenuThemeTest {

    @Test
    public void defaultsHaveExpectedColors() {
        MenuTheme theme = MenuTheme.defaults();
        assertEquals(MenuColor.CYAN, theme.color(Slot.TITLE));
        assertEquals(MenuColor.YELLOW, theme.color(Slot.HINT));
        assertEquals(MenuColor.BLUE, theme.color(Slot.SELECTED_BACKGROUND));
        assertEquals(MenuColor.GREEN, theme.color(Slot.OPTION));
    }

    @Test
    public void withReturnsModifiedCopyAndLeavesOriginalUnchanged() {
        MenuTheme original = MenuTheme.defaults();
        MenuTheme modified = original.with(Slot.TITLE, MenuColor.RED);

        assertNotSame(original, modified);
        assertEquals(MenuColor.RED, modified.color(Slot.TITLE));
        assertEquals("original must be immutable", MenuColor.CYAN, original.color(Slot.TITLE));
        // Other slots are carried over unchanged.
        assertEquals(MenuColor.YELLOW, modified.color(Slot.HINT));
    }
}
