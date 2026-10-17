package com.xsoftwareforge.xconsoleui.list;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.RowActionEvent;

/**
 * A function-key action bound to the highlighted row of a {@link RowBrowser}, or
 * (see {@link #batch}) to the marked rows of a multi-select browser.
 *
 * @param <T> the row element type
 */
public final class RowAction<T> {

    /** Handler invoked with the active context and the highlighted row. */
    @FunctionalInterface
    public interface Handler<T> {
        void run(MenuContext context, T item) throws Exception;
    }

    /**
     * Handler of a batch action: receives the marked rows in list order, or just the
     * cursor row when nothing is marked (or the browser is not multi-select).
     *
     * @since 1.1.0
     */
    @FunctionalInterface
    public interface BatchHandler<T> {
        void run(MenuContext context, List<T> items) throws Exception;
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

    /**
     * Set while a browser runs a batch handler so {@link RowBatch#run} can report each
     * item itself (with its real outcome) instead of the browser reporting the batch.
     */
    static final class BatchScope {
        final String title;
        final FunctionKey key;
        final String label;
        boolean itemsReported;
        boolean cancelled;

        BatchScope(String title, FunctionKey key, String label) {
            this.title = title;
            this.key = key;
            this.label = label;
        }
    }

    static final ThreadLocal<BatchScope> BATCH_SCOPE = new ThreadLocal<>();

    /**
     * Run a batch action on {@code items} and report it to
     * {@link MenuContext#rowActionCompleted}: one event per item. If the handler used
     * {@link RowBatch#run}, that already reported every item with its own outcome;
     * otherwise each item is reported here with the batch's overall outcome.
     *
     * @return {@code false} if the batch was cancelled at its confirmation
     */
    static <T> boolean runBatchReported(MenuContext context, String title, List<T> items,
            Function<T, String> rowText, RowAction<T> action) throws Exception {
        BatchScope scope = new BatchScope(title, action.key(), action.label());
        BatchScope previous = BATCH_SCOPE.get();
        BATCH_SCOPE.set(scope);
        Throwable failure = null;
        try {
            action.batchHandler().run(context, items);
        } catch (Exception | Error ex) {
            failure = ex;
            throw ex;
        } finally {
            BATCH_SCOPE.set(previous);
            if (!scope.itemsReported) {
                for (T item : items) {
                    context.rowActionCompleted(
                            new RowActionEvent(title, rowText.apply(item), action.key(), action.label(), failure));
                }
            }
        }
        return !scope.cancelled;
    }

    private final FunctionKey key;
    private final String label;
    private final Handler<T> handler;
    private final BatchHandler<T> batchHandler;

    public RowAction(FunctionKey key, String label, Handler<T> handler) {
        this(key, label, handler, null);
    }

    private RowAction(FunctionKey key, String label, Handler<T> handler, BatchHandler<T> batchHandler) {
        this.key = Objects.requireNonNull(key, "key");
        this.label = Objects.requireNonNull(label, "label");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.batchHandler = batchHandler;
    }

    /**
     * A batch action: runs once on all marked rows (or the cursor row). Its
     * {@link #handler()} runs the batch handler on a single row.
     *
     * @since 1.1.0
     */
    public static <T> RowAction<T> batch(FunctionKey key, String label, BatchHandler<T> handler) {
        Objects.requireNonNull(handler, "handler");
        return new RowAction<>(key, label, (context, item) -> handler.run(context, List.of(item)), handler);
    }

    /** @since 1.1.0 */
    public boolean isBatch() {
        return batchHandler != null;
    }

    /** The batch handler, or {@code null} for a single-row action. @since 1.1.0 */
    public BatchHandler<T> batchHandler() {
        return batchHandler;
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
