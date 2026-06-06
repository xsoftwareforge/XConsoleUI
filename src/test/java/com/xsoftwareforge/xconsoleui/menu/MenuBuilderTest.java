package com.xsoftwareforge.xconsoleui.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class MenuBuilderTest {

    @Test
    public void buildsStaticMenuInDeclarationOrder() {
        Menu menu = Menu.builder("root", "Root")
                .action("a", "Alpha", ctx -> {
                })
                .action("b", "Beta", OptionColor.RED, ctx -> {
                })
                .build();

        assertEquals("root", menu.id());
        assertEquals("Root", menu.title());

        List<MenuItem> items = menu.items(null);
        assertEquals(2, items.size());
        assertEquals("a", items.get(0).id());
        assertFalse(items.get(0).isSubmenu());
        assertEquals(OptionColor.RED, items.get(1).color());
    }

    @Test
    public void buildsNestedSubmenuInline() {
        Menu menu = Menu.builder("root", "Root")
                .submenu("child", "Child menu", child -> child
                        .action("x", "X", ctx -> {
                        })
                        .action("y", "Y", ctx -> {
                        }))
                .build();

        List<MenuItem> items = menu.items(null);
        assertEquals(1, items.size());

        MenuItem submenuItem = items.get(0);
        assertTrue(submenuItem.isSubmenu());
        assertEquals("child", submenuItem.id());

        Menu child = submenuItem.submenu();
        assertEquals("child", child.id());
        assertEquals("Child menu", child.title());
        assertEquals(List.of("x", "y"), child.items(null).stream().map(MenuItem::id).toList());
    }

    @Test
    public void builtMenuIsImmutable() {
        SimpleMenu menu = Menu.builder("root", "Root")
                .action("a", "Alpha", ctx -> {
                })
                .build();

        List<MenuItem> items = menu.items(null);
        try {
            items.add(MenuItem.action("b", "Beta", ctx -> {
            }));
            throw new AssertionError("expected an immutable item list");
        } catch (UnsupportedOperationException expected) {
            // SimpleMenu copies its items into an unmodifiable list.
        }
    }
}
