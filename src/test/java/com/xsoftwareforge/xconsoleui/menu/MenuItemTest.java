package com.xsoftwareforge.xconsoleui.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MenuItemTest {

    @Test
    public void actionItemIsLeaf() {
        MenuItem item = MenuItem.action("a", "Alpha", context -> {
        });

        assertFalse(item.isSubmenu());
        assertNotNull(item.action());
        assertNull(item.submenu());
        assertEquals(OptionColor.DEFAULT, item.color());
        assertEquals("a", item.id());
        assertEquals("Alpha", item.label());
    }

    @Test
    public void submenuItemReferencesNestedMenu() {
        Menu nested = new StubMenu("nested", "Nested");
        MenuItem item = MenuItem.submenu("s", "Sub", OptionColor.RED, nested);

        assertTrue(item.isSubmenu());
        assertNull(item.action());
        assertEquals(nested, item.submenu());
        assertEquals(OptionColor.RED, item.color());
    }

    @Test(expected = NullPointerException.class)
    public void actionItemRejectsNullAction() {
        MenuItem.action("a", "Alpha", null);
    }

    @Test(expected = NullPointerException.class)
    public void submenuItemRejectsNullMenu() {
        MenuItem.submenu("s", "Sub", null);
    }

    private static final class StubMenu implements Menu {
        private final String id;
        private final String title;

        StubMenu(String id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of();
        }
    }
}
