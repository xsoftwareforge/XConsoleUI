package com.xsoftwareforge.xconsoleui.list;

import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.RowActionEvent;

/**
 * A function-key action bound to the highlighted row of a {@link RowBrowser}.
 *
 * @param <T> the row element type
 */
public final class RowAction<T> {

    /** Handler invoked with the active context and the highlighted row. */
    @FunctionalInterface
    public interface Handler<T> {
        void run(MenuContext context, T item) throws Exception;
    }

    /** A row action body as run by the browsers; used by {@link #runReported}. */
    @FunctionalInterface
    interface Body {
        void run() throws Exception;
    }

    /**
     * Run {@code body} and report its outcome to {@link MenuContext#rowActionCompleted}
     * as soon as it returns; a failure is reported and then rethrown unchanged.
     */
    static void runReported(MenuContext context, String title, String row, FunctionKey key, String label,
            Body body) throws Exception {
        Throwable failure = null;
        try {
            body.run();
        } catch (Exception | Error ex) {
            failure = ex;
            throw ex;
        } finally {
            context.rowActionCompleted(new RowActionEvent(title, row, key, label, failure));
        }
    }

    private final FunctionKey key;
    private final String label;
    private final Handler<T> handler;

    public RowAction(FunctionKey key, String label, Handler<T> handler) {
        this.key = Objects.requireNonNull(key, "key");
        this.label = Objects.requireNonNull(label, "label");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    public FunctionKey key() {
        return key;
    }

    public String label() {
        return label;
    }

    public Handler<T> handler() {
        return handler;
    }
}
