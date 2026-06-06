package com.xsoftwareforge.xconsoleui.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Thread-safe registry of top-level {@link Menu menus}. Each host (a plugin, an
 * application) exposes a single registry; contributors add their menus via
 * {@link #addMenu(Menu)}. Insertion order is preserved so the rendered root menu
 * is stable.
 */
public final class MenuRegistry {

    private final Object lock = new Object();
    private final Map<String, Menu> menus = new LinkedHashMap<>();
    private final Map<String, HotkeyOption> hotkeyOptions = new LinkedHashMap<>();

    /**
     * Register (or replace) a top-level menu. Menus are keyed by
     * {@link Menu#id()}; registering an id that already exists replaces the
     * previous menu while keeping its position.
     *
     * @return the previously registered menu with the same id, or {@code null}
     */
    public Menu addMenu(Menu menu) {
        Objects.requireNonNull(menu, "menu");
        Objects.requireNonNull(menu.id(), "menu.id");
        synchronized (lock) {
            return menus.put(menu.id(), menu);
        }
    }

    /**
     * Remove a previously registered top-level menu.
     *
     * @return the removed menu, or {@code null} if nothing was registered
     */
    public Menu removeMenu(String id) {
        if (id == null) {
            return null;
        }
        synchronized (lock) {
            return menus.remove(id);
        }
    }

    public boolean contains(String id) {
        synchronized (lock) {
            return menus.containsKey(id);
        }
    }

    /** @return a snapshot of the registered menus in registration order. */
    public List<Menu> menus() {
        synchronized (lock) {
            return new ArrayList<>(menus.values());
        }
    }

    /**
     * Register (or replace) a global, assignable hotkey option. Options are keyed
     * by {@link HotkeyOption#id() id}; the user can (re)assign their key in the
     * settings editor. A per-menu {@link Menu#hotkeys() hotkey} still overrides an
     * option for the same effective key while that menu is on screen.
     *
     * @return the previously registered option with the same id, or {@code null}
     */
    public HotkeyOption addHotkeyOption(HotkeyOption option) {
        Objects.requireNonNull(option, "option");
        synchronized (lock) {
            return hotkeyOptions.put(option.id(), option);
        }
    }

    /** Register several hotkey options at once (e.g. {@code BuiltinHotkeyOptions.all(registry)}). */
    public void addHotkeyOptions(List<HotkeyOption> options) {
        Objects.requireNonNull(options, "options");
        for (HotkeyOption option : options) {
            addHotkeyOption(option);
        }
    }

    public HotkeyOption removeHotkeyOption(String id) {
        if (id == null) {
            return null;
        }
        synchronized (lock) {
            return hotkeyOptions.remove(id);
        }
    }

    /** @return a snapshot of the registered hotkey options in registration order. */
    public List<HotkeyOption> hotkeyOptions() {
        synchronized (lock) {
            return new ArrayList<>(hotkeyOptions.values());
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return menus.isEmpty();
        }
    }

    public int size() {
        synchronized (lock) {
            return menus.size();
        }
    }
}
