package com.xsoftwareforge.xconsoleui.menu;

import java.util.List;
import java.util.Objects;

/**
 * A {@link Menu} backed by a fixed, immutable list of items. Useful when the
 * entries are known up front and do not depend on live state. For dynamic menus
 * (entries that change between openings) extend {@link AbstractMenu} instead.
 *
 * @see MenuBuilder
 */
public final class SimpleMenu implements Menu {

    private final String id;
    private final String title;
    private final List<MenuItem> items;
    private final List<Hotkey> hotkeys;

    public SimpleMenu(String id, String title, List<MenuItem> items) {
        this(id, title, items, List.of());
    }

    public SimpleMenu(String id, String title, List<MenuItem> items, List<Hotkey> hotkeys) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.hotkeys = List.copyOf(Objects.requireNonNull(hotkeys, "hotkeys"));
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
        return items;
    }

    @Override
    public List<Hotkey> hotkeys() {
        return hotkeys;
    }
}
