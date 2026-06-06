package com.xsoftwareforge.xconsoleui.config;

import java.util.EnumMap;
import java.util.Objects;

/**
 * Immutable color theme for the menu screens. A theme assigns a {@link MenuColor}
 * to each {@link Slot}; {@link #with(Slot, MenuColor)} returns a modified copy.
 */
public final class MenuTheme {

    /** Styleable elements of a screen. */
    public enum Slot {
        TITLE,
        HINT,
        HOTKEY,
        FOOTER,
        CONTENT,
        SELECTED_FOREGROUND,
        SELECTED_BACKGROUND,
        OPTION
    }

    private final EnumMap<Slot, MenuColor> colors;

    private MenuTheme(EnumMap<Slot, MenuColor> colors) {
        this.colors = colors;
    }

    /** The built-in default theme (cyan title, yellow hints, blue selection, ...). */
    public static MenuTheme defaults() {
        EnumMap<Slot, MenuColor> colors = new EnumMap<>(Slot.class);
        colors.put(Slot.TITLE, MenuColor.CYAN);
        colors.put(Slot.HINT, MenuColor.YELLOW);
        colors.put(Slot.HOTKEY, MenuColor.CYAN);
        colors.put(Slot.FOOTER, MenuColor.MAGENTA);
        colors.put(Slot.CONTENT, MenuColor.WHITE);
        colors.put(Slot.SELECTED_FOREGROUND, MenuColor.WHITE);
        colors.put(Slot.SELECTED_BACKGROUND, MenuColor.BLUE);
        colors.put(Slot.OPTION, MenuColor.GREEN);
        return new MenuTheme(colors);
    }

    public MenuColor color(Slot slot) {
        return colors.getOrDefault(slot, MenuColor.DEFAULT);
    }

    /** Return a copy of this theme with {@code slot} set to {@code color}. */
    public MenuTheme with(Slot slot, MenuColor color) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(color, "color");
        EnumMap<Slot, MenuColor> copy = new EnumMap<>(colors);
        copy.put(slot, color);
        return new MenuTheme(copy);
    }
}
