package com.xsoftwareforge.xconsoleui.menu;

import java.util.Objects;

/**
 * Binds a {@link FunctionKey} to a labelled {@link MenuAction}. Hotkeys can be
 * registered globally on a {@link MenuRegistry} or per-menu via
 * {@link Menu#hotkeys()}; a per-menu hotkey overrides a global one for the same
 * key while that menu is on screen.
 *
 * @param key    the function key that triggers the action
 * @param label  short text shown in the hotkey hint line (e.g. "Help")
 * @param action the action to run when the key is pressed
 */
public record Hotkey(FunctionKey key, String label, MenuAction action) {

    public Hotkey {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
    }
}
