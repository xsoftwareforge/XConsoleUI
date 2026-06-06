package com.xsoftwareforge.xconsoleui.menu;

import java.util.Objects;

/**
 * A single navigable entry in a {@link Menu}. An item is either:
 * <ul>
 *   <li>a <em>leaf</em> carrying a {@link MenuAction} that runs when selected, or</li>
 *   <li>a <em>sub-menu</em> referencing a nested {@link Menu} that is opened when
 *       selected.</li>
 * </ul>
 * Instances are immutable and created through the static factory methods.
 */
public final class MenuItem {

    private final String id;
    private final String label;
    private final OptionColor color;
    private final MenuAction action;
    private final Menu submenu;

    private MenuItem(String id, String label, OptionColor color, MenuAction action, Menu submenu) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.color = color == null ? OptionColor.DEFAULT : color;
        this.action = action;
        this.submenu = submenu;
    }

    /** Create a leaf item that runs {@code action} when selected. */
    public static MenuItem action(String id, String label, MenuAction action) {
        return action(id, label, OptionColor.DEFAULT, action);
    }

    /** Create a colored leaf item that runs {@code action} when selected. */
    public static MenuItem action(String id, String label, OptionColor color, MenuAction action) {
        return new MenuItem(id, label, color, Objects.requireNonNull(action, "action"), null);
    }

    /** Create an item that opens the nested {@code submenu} when selected. */
    public static MenuItem submenu(String id, String label, Menu submenu) {
        return submenu(id, label, OptionColor.DEFAULT, submenu);
    }

    /** Create a colored item that opens the nested {@code submenu} when selected. */
    public static MenuItem submenu(String id, String label, OptionColor color, Menu submenu) {
        return new MenuItem(id, label, color, null, Objects.requireNonNull(submenu, "submenu"));
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public OptionColor color() {
        return color;
    }

    /** @return the action of a leaf item, or {@code null} for a sub-menu item. */
    public MenuAction action() {
        return action;
    }

    /** @return the nested menu of a sub-menu item, or {@code null} for a leaf. */
    public Menu submenu() {
        return submenu;
    }

    public boolean isSubmenu() {
        return submenu != null;
    }

    @Override
    public String toString() {
        return "MenuItem[id=" + id + ", label=" + label + ", "
                + (isSubmenu() ? "submenu" : "action") + "]";
    }
}
