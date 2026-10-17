package com.xsoftwareforge.xconsoleui.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.RowActionEvent;

/**
 * Runs a per-item action over a batch of rows (typically the selection handed to a
 * {@link RowAction.BatchHandler}): asks once for confirmation when there is more
 * than one item, runs the action on each item, and shows a summary of the outcomes
 * via {@link MenuContext#showResult}.
 *
 * <pre>{@code
 * browser.multiSelect(true).batchAction(FunctionKey.F5, "Löschen", (ctx, rows) ->
 *         RowBatch.run(ctx, "Löschen", rows, Asteroid::name, (c, a) -> {
 *             if (a.occupied()) {
 *                 throw new RowBatch.Skip("besetzt");
 *             }
 *             service.delete(a);
 *         }));
 * }</pre>
 *
 * Audit hooks: when called from a browser's batch action, each item is reported to
 * {@link MenuContext#rowActionCompleted} with its own outcome ({@code error} is
 * {@code null} for OK, the {@link Skip} for a skipped item, the exception for a
 * failed one); a cancelled confirmation reports nothing. Called outside a browser,
 * nothing is reported.
 *
 * @since 1.1.0
 */
public final class RowBatch {

    /** Thrown by the per-item action to skip an item with a reason (not a failure). */
    public static final class Skip extends Exception {
        private static final long serialVersionUID = 1L;

        public Skip(String reason) {
            super(reason);
        }
    }

    public enum Status {
        OK,
        SKIPPED,
        FAILED
    }

    /** Outcome of one item: its description, status and the skip reason / error message. */
    public record Outcome(String item, Status status, String detail) {
    }

    private RowBatch() {
    }

    /**
     * Run {@code action} on every item and show the summary under {@code title}.
     * Interrupts (Ctrl+C/EOF) abort the batch and propagate.
     *
     * @param describe how an item is named in the summary (and in audit events)
     * @return the outcome per item, or an empty list if there were no items or the
     *         user declined the confirmation
     */
    public static <T> List<Outcome> run(MenuContext context, String title, List<T> items,
            Function<T, String> describe, RowAction.Handler<T> action) throws Exception {
        Objects.requireNonNull(describe, "describe");
        Objects.requireNonNull(action, "action");
        RowAction.BatchScope scope = RowAction.BATCH_SCOPE.get();
        if (scope != null) {
            scope.itemsReported = true;
        }
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (items.size() > 1 && !context.confirm(title + ": Für " + items.size() + " Einträge ausführen?")) {
            if (scope != null) {
                scope.cancelled = true;
            }
            return List.of();
        }
        List<Outcome> outcomes = new ArrayList<>(items.size());
        for (T item : items) {
            String name = describe.apply(item);
            Exception error = null;
            try {
                action.run(context, item);
                outcomes.add(new Outcome(name, Status.OK, ""));
            } catch (UserInterruptException | EndOfFileException ex) {
                throw ex;
            } catch (Skip skip) {
                error = skip;
                outcomes.add(new Outcome(name, Status.SKIPPED, String.valueOf(skip.getMessage())));
            } catch (Exception ex) {
                error = ex;
                outcomes.add(new Outcome(name, Status.FAILED, ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            }
            if (scope != null) {
                context.rowActionCompleted(new RowActionEvent(scope.title, name, scope.key, scope.label, error));
            }
        }
        context.showResult(title, summary(outcomes));
        return outcomes;
    }

    /** The summary lines shown by {@link #run}: a count line, a blank line, one line per item. */
    static List<String> summary(List<Outcome> outcomes) {
        int ok = 0;
        int skipped = 0;
        int failed = 0;
        for (Outcome outcome : outcomes) {
            switch (outcome.status()) {
            case OK -> ok++;
            case SKIPPED -> skipped++;
            case FAILED -> failed++;
            }
        }
        List<String> lines = new ArrayList<>(outcomes.size() + 2);
        lines.add(ok + " ok, " + skipped + " übersprungen, " + failed + " fehlgeschlagen");
        lines.add("");
        for (Outcome outcome : outcomes) {
            lines.add(switch (outcome.status()) {
            case OK -> "OK            " + outcome.item();
            case SKIPPED -> "ÜBERSPRUNGEN  " + outcome.item() + ": " + outcome.detail();
            case FAILED -> "FEHLER        " + outcome.item() + ": " + outcome.detail();
            });
        }
        return lines;
    }
}
