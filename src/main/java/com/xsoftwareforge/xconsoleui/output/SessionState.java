package com.xsoftwareforge.xconsoleui.output;

/**
 * Interaction states tracked by the {@link ConsoleOutputCoordinator}. While a
 * buffering state is active on the stack, asynchronous output is held back and
 * replayed once the stack returns to {@link #IDLE}.
 */
public enum SessionState {

    /** No printer attached; output falls back to {@code System.out}. */
    DISABLED,

    /** Printer attached, nothing interactive in progress; output flows live. */
    IDLE,

    /** A navigable menu is on screen; output is buffered. */
    MENU,

    /** A multi-step prompt/wizard is on screen; output is buffered. */
    WIZARD,

    /** Shutdown in progress; output is buffered until the printer detaches. */
    SHUTDOWN;

    /** @return whether output must be buffered while this state is active. */
    public boolean buffers() {
        return this == MENU || this == WIZARD || this == SHUTDOWN;
    }
}
