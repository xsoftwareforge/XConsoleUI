package com.xsoftwareforge.xconsoleui.menu;

import java.util.Objects;

/**
 * A named, assignable global hotkey action. Unlike a per-menu {@link Hotkey}
 * (which is fixed to its key), an option has a stable {@link #id() id} so it can
 * be (re)assigned to a function key in the settings editor and persisted — even
 * if it starts out unbound.
 *
 * @param id         stable identity used for assignment/persistence
 * @param label      text shown in the hotkey hint line and the settings editor
 * @param defaultKey the key bound out of the box, or {@code null} to start unbound
 * @param action     the action to run when the (effective) key is pressed
 */
public record HotkeyOption(String id, String label, FunctionKey defaultKey, MenuAction action) {

    public HotkeyOption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(action, "action");
        // defaultKey is intentionally nullable (an option may start unbound).
    }

    /** Create an option that starts unbound (no default key). */
    public static HotkeyOption of(String id, String label, MenuAction action) {
        return new HotkeyOption(id, label, null, action);
    }

    /** Create an option bound to {@code defaultKey} out of the box. */
    public static HotkeyOption of(String id, String label, FunctionKey defaultKey, MenuAction action) {
        return new HotkeyOption(id, label, defaultKey, action);
    }
}
