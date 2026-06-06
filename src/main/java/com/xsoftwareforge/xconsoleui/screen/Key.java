package com.xsoftwareforge.xconsoleui.screen;

/**
 * The logical keys an interactive screen reacts to, decoded from raw terminal
 * input by {@link KeyParser}. Cursor keys, navigation, the function keys
 * {@code F1}-{@code F12} plus the synthetic {@link #RESIZE}, {@link #INTERRUPT}
 * and {@link #EOF} signals.
 */
enum Key {
    UP, DOWN, LEFT, RIGHT, HOME, END, PAGE_UP, PAGE_DOWN, TAB,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,
    ENTER, BACKSPACE, ESCAPE, CHARACTER, RESIZE, UNKNOWN, INTERRUPT, EOF
}
