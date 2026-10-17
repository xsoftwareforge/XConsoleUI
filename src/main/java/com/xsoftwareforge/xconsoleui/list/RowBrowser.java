package com.xsoftwareforge.xconsoleui.list;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.BrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/**
 * High-level interactive list of {@code T} rows: cursor selection, type-ahead
 * filtering, and per-row function-key actions. The canonical use is an admin list
 * such as "list sockets" where each row can be acted on (kick, inspect, ...).
 * <p>
 * The row data is read from a {@link Supplier} on every redraw, so changes made
 * by a row action (e.g. removing an entry) are reflected immediately.
 * <p>
 * With {@link #multiSelect(boolean) multiSelect(true)} rows can be marked (Space,
 * Ctrl+A for all filtered rows, Esc clears marks first) and {@link #batchAction}s
 * receive all marked rows. Space then toggles marks instead of extending the
 * type-ahead filter; all other characters still filter. Marks are kept per row
 * element (by {@code equals}) and survive filter changes and redraws.
 *
 * @param <T> the row element type
 */
public final class RowBrowser<T> {

    private final String title;
    private final Supplier<List<T>> source;
    private final Function<T, String> renderer;
    private final Map<FunctionKey, RowAction<T>> actions = new LinkedHashMap<>();
    private boolean filterable = true;
    private boolean multiSelect;
    private RowAction.Handler<T> onSelect;

    private RowBrowser(String title, Supplier<List<T>> source, Function<T, String> renderer) {
        this.title = Objects.requireNonNull(title, "title");
        this.source = Objects.requireNonNull(source, "source");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public static <T> RowBrowser<T> create(String title, List<T> items, Function<T, String> renderer) {
        List<T> snapshot = List.copyOf(items);
        return new RowBrowser<>(title, () -> snapshot, renderer);
    }

    /** Use a live supplier so row actions that mutate the data show up on redraw. */
    public static <T> RowBrowser<T> create(String title, Supplier<List<T>> source, Function<T, String> renderer) {
        return new RowBrowser<>(title, source, renderer);
    }

    public RowBrowser<T> filterable(boolean filterable) {
        this.filterable = filterable;
        return this;
    }

    public RowBrowser<T> action(FunctionKey key, String label, RowAction.Handler<T> handler) {
        actions.put(key, new RowAction<>(key, label, handler));
        return this;
    }

    /**
     * Enable row marking (Space / Ctrl+A / Esc). Off by default, so a browser behaves
     * exactly as in 1.0 unless it opts in.
     *
     * @since 1.1.0
     */
    public RowBrowser<T> multiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
        return this;
    }

    /**
     * Function-key action on the marked rows (in list order), or just the cursor row
     * when nothing is marked. The marks are cleared once the handler returns, unless
     * it threw or a {@link RowBatch#run} confirmation was declined. See
     * {@link RowBatch} for a ready-made confirm/run/summary helper.
     *
     * @since 1.1.0
     */
    public RowBrowser<T> batchAction(FunctionKey key, String label, RowAction.BatchHandler<T> handler) {
        actions.put(key, RowAction.batch(key, label, handler));
        return this;
    }

    /** Action run when Enter is pressed on a row (instead of returning from {@link #open}). */
    public RowBrowser<T> onSelect(RowAction.Handler<T> handler) {
        this.onSelect = handler;
        return this;
    }

    /**
     * Open the browser and loop until the user goes back (Esc). Row actions run in
     * place; if an {@link #onSelect} handler is set, Enter runs it, otherwise Enter
     * closes the browser.
     */
    public void open(MenuContext context) throws Exception {
        Map<FunctionKey, String> hints = hints();
        BrowseState state = multiSelect ? new BrowseState() : null;
        Marks<T> marks = new Marks<>();
        while (true) {
            List<T> items = source.get();
            BrowseResult result;
            if (state == null) {
                result = context.browse(title, render(items), hints, filterable);
            } else {
                marks.toState(items, state);
                result = context.browse(title, render(items), hints, filterable, state);
                marks.fromState(items, state);
            }
            switch (result.kind()) {
            case BACK -> {
                return;
            }
            case SELECT -> {
                if (onSelect == null) {
                    return;
                }
                if (inRange(items, result.index())) {
                    T item = items.get(result.index());
                    RowAction.runReported(context, title, renderer.apply(item), null, "Select",
                            () -> onSelect.run(context, item));
                }
            }
            case ACTION -> {
                RowAction<T> action = actions.get(result.actionKey());
                if (action != null && inRange(items, result.index())) {
                    T item = items.get(result.index());
                    if (action.isBatch()) {
                        if (RowAction.runBatchReported(context, title, marks.targets(items, item), renderer,
                                action)) {
                            marks.clear();
                        }
                    } else {
                        RowAction.runReported(context, title, renderer.apply(item), action.key(), action.label(),
                                () -> action.handler().run(context, item));
                    }
                }
            }
            }
        }
    }

    /**
     * Open the browser purely as a picker: returns the chosen element, or empty if
     * the user went back. Row actions are ignored for picking.
     */
    public Optional<T> pick(MenuContext context) throws Exception {
        List<T> items = source.get();
        BrowseResult result = context.browse(title, render(items), Map.of(), filterable);
        if (result.kind() == BrowseResult.Kind.SELECT && inRange(items, result.index())) {
            return Optional.of(items.get(result.index()));
        }
        return Optional.empty();
    }

    private List<String> render(List<T> items) {
        List<String> rows = new java.util.ArrayList<>(items.size());
        for (T item : items) {
            rows.add(renderer.apply(item));
        }
        return rows;
    }

    private Map<FunctionKey, String> hints() {
        Map<FunctionKey, String> hints = new LinkedHashMap<>();
        for (RowAction<T> action : actions.values()) {
            hints.put(action.key(), action.label());
        }
        return hints;
    }

    private static boolean inRange(List<?> items, int index) {
        return index >= 0 && index < items.size();
    }
}
