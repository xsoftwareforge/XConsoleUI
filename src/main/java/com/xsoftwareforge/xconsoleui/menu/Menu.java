package com.xsoftwareforge.xconsoleui.menu;

import java.util.List;

/**
 * A registerable menu node. A menu has a stable {@link #id() id}, a
 * {@link #title() title} shown as the screen header, and a list of
 * {@link MenuItem items} that is resolved every time the menu is opened so the
 * contents can react to live state.
 * <p>
 * Consumers typically extend {@link AbstractMenu} instead of implementing this
 * interface directly, and register an instance via
 * {@link MenuRegistry#addMenu(Menu)}.
 */
public interface Menu {

    /**
     * Start building a static menu fluently.
     *
     * @see MenuBuilder
     */
    static MenuBuilder builder(String id, String title) {
        return new MenuBuilder(id, title);
    }

    /** Stable, unique identifier used for registration and navigation. */
    String id();

    /** Header shown at the top of the menu screen. */
    String title();

    /**
     * Resolve the items of this menu. Called each time the menu is rendered, so
     * implementations may return a freshly built, state-dependent list.
     *
     * @param context the active menu context (never {@code null})
     * @return the items to display; an empty list renders an empty menu
     */
    List<MenuItem> items(MenuContext context);

    /**
     * Function-key actions active while this menu is on screen. A hotkey here
     * overrides a {@link MenuRegistry#hotkeys() global} hotkey bound to the same
     * {@link FunctionKey}. Defaults to none.
     */
    default List<Hotkey> hotkeys() {
        return List.of();
    }
}
