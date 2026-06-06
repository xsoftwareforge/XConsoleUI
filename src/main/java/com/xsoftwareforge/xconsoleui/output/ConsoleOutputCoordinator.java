package com.xsoftwareforge.xconsoleui.output;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Coordinates asynchronous console output with interactive screens.
 * <p>
 * While a buffering {@link SessionState} (menu, wizard, shutdown) is active,
 * {@link #publish(String)} stores messages instead of writing them, preventing
 * log lines from corrupting a rendered menu. When the interaction stack drains
 * back to {@link SessionState#IDLE} the buffer is flushed in arrival order.
 * <p>
 * Interaction states are kept on a stack so nested screens (a menu opening a
 * wizard) behave correctly: buffering stays on until the outermost interaction
 * leaves. The class is thread-safe; the worker thread driving the menu and the
 * threads producing log output may call it concurrently.
 * <p>
 * Unlike a global singleton, instances are independent, which keeps the buffering
 * logic unit-testable and lets a host run more than one coordinator if needed.
 */
public final class ConsoleOutputCoordinator {

    /** Default maximum number of lines kept in the scrollback history. */
    private static final int DEFAULT_MAX_HISTORY = 1000;

    private final Object monitor = new Object();
    private final List<String> buffer = new ArrayList<>();
    private final Deque<SessionState> stateStack = new ArrayDeque<>();
    private final Deque<String> history = new ArrayDeque<>();
    private final int maxHistory;

    /** Create a coordinator with the default scrollback limit ({@value #DEFAULT_MAX_HISTORY} lines). */
    public ConsoleOutputCoordinator() {
        this(DEFAULT_MAX_HISTORY);
    }

    /**
     * Create a coordinator that keeps at most {@code maxHistory} lines of scrollback.
     *
     * @param maxHistory the scrollback limit; must be positive
     */
    public ConsoleOutputCoordinator(int maxHistory) {
        if (maxHistory <= 0) {
            throw new IllegalArgumentException("maxHistory must be positive: " + maxHistory);
        }
        this.maxHistory = maxHistory;
    }

    private volatile OutputPrinter printer;
    private volatile SessionState state = SessionState.DISABLED;

    /**
     * Attach the output sink and move to {@link SessionState#IDLE}, then flush any
     * output that was buffered while detached.
     */
    public void attachPrinter(OutputPrinter outputPrinter) {
        synchronized (monitor) {
            printer = outputPrinter;
            stateStack.clear();
            state = SessionState.IDLE;
        }
        flush();
    }

    /**
     * Flush remaining output and detach the sink, returning to
     * {@link SessionState#DISABLED}.
     */
    public void detachPrinter() {
        flush();
        synchronized (monitor) {
            printer = null;
            stateStack.clear();
            state = SessionState.DISABLED;
        }
    }

    public SessionState getState() {
        return state;
    }

    /**
     * Emit a formatted message. Written immediately when idle, buffered while an
     * interactive screen is active.
     */
    public void publish(String formattedMessage) {
        if (formattedMessage == null || formattedMessage.isEmpty()) {
            return;
        }

        OutputPrinter currentPrinter;
        synchronized (monitor) {
            recordHistory(formattedMessage);
            if (shouldBuffer()) {
                buffer.add(formattedMessage);
                return;
            }
            currentPrinter = printer;
        }
        write(formattedMessage, currentPrinter);
    }

    /** Push an interactive state; subsequent output is buffered while it buffers. */
    public void enterInteraction(SessionState nextState) {
        synchronized (monitor) {
            stateStack.addLast(nextState);
            state = nextState;
        }
    }

    /**
     * Pop the most recent interactive state. When the stack empties the state
     * returns to {@code idleState} and buffered output is flushed (unless we are
     * shutting down).
     */
    public void leaveInteraction(SessionState idleState) {
        boolean shouldFlush;
        synchronized (monitor) {
            if (!stateStack.isEmpty()) {
                stateStack.removeLast();
            }
            state = stateStack.isEmpty() ? idleState : stateStack.peekLast();
            shouldFlush = stateStack.isEmpty() && state != SessionState.SHUTDOWN;
        }
        if (shouldFlush) {
            flush();
        }
    }

    /** Replay and clear any buffered output through the current printer. */
    public void flush() {
        List<String> drained;
        OutputPrinter currentPrinter;
        synchronized (monitor) {
            if (buffer.isEmpty()) {
                return;
            }
            drained = new ArrayList<>(buffer);
            buffer.clear();
            currentPrinter = printer;
        }
        for (String message : drained) {
            write(message, currentPrinter);
        }
    }

    /** @return a snapshot of the recent output history (oldest first). */
    public List<String> history() {
        synchronized (monitor) {
            return new ArrayList<>(history);
        }
    }

    /** Clear the output history. */
    public void clearHistory() {
        synchronized (monitor) {
            history.clear();
        }
    }

    private void recordHistory(String message) {
        String trimmed = trimTrailingLineBreaks(message);
        for (String line : trimmed.split("\n", -1)) {
            history.addLast(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
            if (history.size() > maxHistory) {
                history.removeFirst();
            }
        }
    }

    private void write(String message, OutputPrinter currentPrinter) {
        if (currentPrinter != null) {
            currentPrinter.printAbove(trimTrailingLineBreaks(message));
            return;
        }
        System.out.print(message);
    }

    private boolean shouldBuffer() {
        if (state == SessionState.SHUTDOWN) {
            return true;
        }
        for (SessionState stackedState : stateStack) {
            if (stackedState.buffers()) {
                return true;
            }
        }
        return false;
    }

    private static String trimTrailingLineBreaks(String message) {
        String value = message;
        while (value.endsWith("\n") || value.endsWith("\r")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
