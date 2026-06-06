package com.xsoftwareforge.xconsoleui.screen;

import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.FunctionKey;

/**
 * Outcome of a hotkey-aware selection screen: the user either picked an item,
 * pressed a bound function key, or went back (Esc/q).
 */
public final class ScreenChoice {

    private static final ScreenChoice BACK = new ScreenChoice(null, null, false);
    private static final ScreenChoice TOGGLE = new ScreenChoice(null, null, true);

    private final String itemId;
    private final FunctionKey hotkey;
    private final boolean toggleView;

    private ScreenChoice(String itemId, FunctionKey hotkey, boolean toggleView) {
        this.itemId = itemId;
        this.hotkey = hotkey;
        this.toggleView = toggleView;
    }

    public static ScreenChoice item(String id) {
        return new ScreenChoice(Objects.requireNonNull(id, "id"), null, false);
    }

    public static ScreenChoice hotkey(FunctionKey key) {
        return new ScreenChoice(null, Objects.requireNonNull(key, "key"), false);
    }

    public static ScreenChoice back() {
        return BACK;
    }

    /** The user requested to toggle this list's layout (LIST/GRID). */
    public static ScreenChoice toggleView() {
        return TOGGLE;
    }

    public boolean isItem() {
        return itemId != null;
    }

    public boolean isHotkey() {
        return hotkey != null;
    }

    public boolean isToggleView() {
        return toggleView;
    }

    public boolean isBack() {
        return itemId == null && hotkey == null && !toggleView;
    }

    public String itemId() {
        return itemId;
    }

    public FunctionKey hotkey() {
        return hotkey;
    }
}
