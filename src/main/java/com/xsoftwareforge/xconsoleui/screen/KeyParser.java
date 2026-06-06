package com.xsoftwareforge.xconsoleui.screen;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.utils.NonBlockingReader;

/**
 * Decodes raw terminal bytes into logical {@link Key} values: single control
 * characters (Enter, Backspace, Tab, Ctrl+C) and CSI/SS3 escape sequences for
 * the cursor, navigation and function keys.
 * <p>
 * The parser is stateful only in that it remembers the {@linkplain #lastCharacter()
 * last printable character} read (so callers can implement type-ahead filters and
 * the {@code q} quit shortcut) and observes a shared {@code resizePending} flag
 * set by the host's {@code WINCH} handler. The pure code-point mappers
 * ({@link #mapLetter(int)} and {@link #mapNumberKey(int)}) are package-visible and
 * unit-tested directly.
 */
final class KeyParser {

    /**
     * Time to wait for the remaining bytes of an escape sequence after the initial
     * ESC. Generous enough that a multi-byte arrow-key sequence is not mistaken for
     * a lone Esc press on slower input pipelines, yet short enough that a real Esc
     * still feels instant.
     */
    private static final long ESC_SEQUENCE_TIMEOUT_MS = 100L;

    /**
     * How long the initial key read waits before checking for a pending terminal
     * resize. Keeps resize handling responsive without busy-spinning.
     */
    private static final long POLL_TIMEOUT_MS = 150L;

    // Named control codes, replacing inline ASCII magic numbers.
    private static final int CTRL_C = 3;
    private static final int BACKSPACE = 8;
    private static final int TAB = 9;
    private static final int LINE_FEED = '\n';
    private static final int CARRIAGE_RETURN = '\r';
    private static final int ESC = 27;
    private static final int DEL = 127;

    private final AtomicBoolean resizePending;
    private Character lastCharacter;

    KeyParser(AtomicBoolean resizePending) {
        this.resizePending = Objects.requireNonNull(resizePending, "resizePending");
    }

    /** The last printable character decoded by {@link #readKey(NonBlockingReader)}, or {@code null}. */
    Character lastCharacter() {
        return lastCharacter;
    }

    /** @return {@code true} if the last read character was {@code q}/{@code Q} (the back/quit shortcut). */
    boolean isQuitCharacter() {
        Character printable = lastCharacter;
        return printable != null && (printable == 'q' || printable == 'Q');
    }

    /** Block until the next key is available (or a resize/EOF/interrupt signal occurs). */
    Key readKey(NonBlockingReader reader) throws IOException {
        lastCharacter = null;
        while (true) {
            int ch = reader.read(POLL_TIMEOUT_MS);
            if (ch == NonBlockingReader.READ_EXPIRED) {
                if (resizePending.compareAndSet(true, false)) {
                    return Key.RESIZE;
                }
                continue;
            }
            if (ch == -1) {
                return Key.EOF;
            }
            if (ch == CTRL_C) {
                return Key.INTERRUPT;
            }
            if (ch == CARRIAGE_RETURN || ch == LINE_FEED) {
                return Key.ENTER;
            }
            if (ch == DEL || ch == BACKSPACE) {
                return Key.BACKSPACE;
            }
            if (ch == TAB) {
                return Key.TAB;
            }
            if (ch == ESC) {
                int next = reader.read(ESC_SEQUENCE_TIMEOUT_MS);
                if (next == NonBlockingReader.READ_EXPIRED || next == -1) {
                    return Key.ESCAPE;
                }
                // CSI ("ESC [") and SS3 ("ESC O", application cursor mode) both
                // introduce cursor/navigation keys; some terminals use one or the
                // other for the arrow keys.
                if (next == '[' || next == 'O') {
                    return readControlSequence(reader);
                }
                return Key.ESCAPE;
            }
            lastCharacter = (char) ch;
            return Key.CHARACTER;
        }
    }

    /**
     * Parse the body of a control sequence after the {@code ESC [} (CSI) or
     * {@code ESC O} (SS3) introducer. Handles letter-terminated keys (arrows,
     * Home/End, and SS3 function keys F1-F4) as well as numeric CSI sequences such
     * as {@code ESC [ 1 5 ~} (F5), tolerating {@code ;} modifier parameters.
     */
    private Key readControlSequence(NonBlockingReader reader) throws IOException {
        int b = reader.read(ESC_SEQUENCE_TIMEOUT_MS);
        if (b == NonBlockingReader.READ_EXPIRED || b == -1) {
            return Key.ESCAPE;
        }

        Key letter = mapLetter(b);
        if (letter != null) {
            return letter;
        }

        if (b >= '0' && b <= '9') {
            int number = b - '0';
            int t = reader.read(ESC_SEQUENCE_TIMEOUT_MS);
            while (t >= '0' && t <= '9') {
                number = number * 10 + (t - '0');
                t = reader.read(ESC_SEQUENCE_TIMEOUT_MS);
            }
            if (t == ';') {
                // Skip modifier parameters (e.g. Ctrl/Shift) up to the final byte.
                int m = reader.read(ESC_SEQUENCE_TIMEOUT_MS);
                while (m != -1 && m != NonBlockingReader.READ_EXPIRED && m != '~' && mapLetter(m) == null) {
                    m = reader.read(ESC_SEQUENCE_TIMEOUT_MS);
                }
                Key modifiedLetter = mapLetter(m);
                if (modifiedLetter != null) {
                    return modifiedLetter;
                }
            }
            return mapNumberKey(number);
        }

        return Key.UNKNOWN;
    }

    /** Map a letter-terminated CSI/SS3 key. Returns {@code null} if not a letter key. */
    static Key mapLetter(int b) {
        return switch (b) {
        case 'A' -> Key.UP;
        case 'B' -> Key.DOWN;
        case 'C' -> Key.RIGHT;
        case 'D' -> Key.LEFT;
        case 'H' -> Key.HOME;
        case 'F' -> Key.END;
        case 'P' -> Key.F1;
        case 'Q' -> Key.F2;
        case 'R' -> Key.F3;
        case 'S' -> Key.F4;
        default -> null;
        };
    }

    /** Map a numeric CSI key (the value before the terminating {@code ~}). */
    static Key mapNumberKey(int number) {
        return switch (number) {
        case 1, 7 -> Key.HOME;
        case 4, 8 -> Key.END;
        case 5 -> Key.PAGE_UP;
        case 6 -> Key.PAGE_DOWN;
        case 11 -> Key.F1;
        case 12 -> Key.F2;
        case 13 -> Key.F3;
        case 14 -> Key.F4;
        case 15 -> Key.F5;
        case 17 -> Key.F6;
        case 18 -> Key.F7;
        case 19 -> Key.F8;
        case 20 -> Key.F9;
        case 21 -> Key.F10;
        case 23 -> Key.F11;
        case 24 -> Key.F12;
        default -> Key.UNKNOWN; // e.g. 2 = Insert, 3 = Delete: ignored
        };
    }
}
