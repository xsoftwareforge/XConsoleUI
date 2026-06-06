package com.xsoftwareforge.xconsoleui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent builder for static menus. Produces an immutable {@link SimpleMenu}
 * without requiring a dedicated subclass:
 *
 * <pre>{@code
 * Menu menu = Menu.builder("tools", "Tools")
 *     .action("ping", "Ping", ctx -> ctx.printSuccess("pong"))
 *     .submenu("more", "More...", more -> more
 *         .action("about", "About", ctx -> ctx.print("v1")))
 *     .build();
 * }</pre>
 *
 * Obtain an instance via {@link Menu#builder(String, String)}.
 */
public final class MenuBuilder {

    private final String id;
    private final String title;
    private final List<MenuItem> items = new ArrayList<>();
    private final List<Hotkey> hotkeys = new ArrayList<>();

    MenuBuilder(String id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
    }

    /** Append an already-constructed item. */
    public MenuBuilder item(MenuItem item) {
        items.add(Objects.requireNonNull(item, "item"));
        return this;
    }

    public MenuBuilder action(String id, String label, MenuAction action) {
        return item(MenuItem.action(id, label, action));
    }

    public MenuBuilder action(String id, String label, OptionColor color, MenuAction action) {
        return item(MenuItem.action(id, label, color, action));
    }

    /** Append a sub-menu item referencing an existing menu. */
    public MenuBuilder submenu(String id, String label, Menu submenu) {
        return item(MenuItem.submenu(id, label, submenu));
    }

    public MenuBuilder submenu(String id, String label, OptionColor color, Menu submenu) {
        return item(MenuItem.submenu(id, label, color, submenu));
    }

    /**
     * Append a sub-menu built inline. The nested menu reuses {@code id}/{@code label}
     * as its own id/title; {@code configurer} fills in its items.
     */
    public MenuBuilder submenu(String id, String label, Consumer<MenuBuilder> configurer) {
        return submenu(id, label, OptionColor.DEFAULT, configurer);
    }

    public MenuBuilder submenu(String id, String label, OptionColor color, Consumer<MenuBuilder> configurer) {
        Objects.requireNonNull(configurer, "configurer");
        MenuBuilder child = new MenuBuilder(id, label);
        configurer.accept(child);
        return submenu(id, label, color, child.build());
    }

    /** Bind a function key to an action that is active while this menu is shown. */
    public MenuBuilder hotkey(FunctionKey key, String label, MenuAction action) {
        hotkeys.add(new Hotkey(key, label, action));
        return this;
    }

    public SimpleMenu build() {
        return new SimpleMenu(id, title, items, hotkeys);
    }
}
