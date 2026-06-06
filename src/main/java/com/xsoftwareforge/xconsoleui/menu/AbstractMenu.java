package com.xsoftwareforge.xconsoleui.menu;

import java.util.Objects;

/**
 * Convenience base class for {@link Menu} implementations. Holds the id and
 * title and leaves only {@link #items(MenuContext)} for subclasses to implement.
 * <p>
 * Static helpers {@link #action(String, String, MenuAction)} and
 * {@link #submenu(String, String, Menu)} mirror the {@link MenuItem} factories
 * so subclasses can build their item lists fluently.
 */
public abstract class AbstractMenu implements Menu {

    private final String id;
    private final String title;

    protected AbstractMenu(String id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final String title() {
        return title;
    }

    protected static MenuItem action(String id, String label, MenuAction action) {
        return MenuItem.action(id, label, action);
    }

    protected static MenuItem action(String id, String label, OptionColor color, MenuAction action) {
        return MenuItem.action(id, label, color, action);
    }

    protected static MenuItem submenu(String id, String label, Menu submenu) {
        return MenuItem.submenu(id, label, submenu);
    }

    protected static MenuItem submenu(String id, String label, OptionColor color, Menu submenu) {
        return MenuItem.submenu(id, label, color, submenu);
    }
}
