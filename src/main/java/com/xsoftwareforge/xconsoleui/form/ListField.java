package com.xsoftwareforge.xconsoleui.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.OptionColor;

/**
 * A repeatable list field: a managed list of elements the user can add, edit,
 * and remove — the generalization of the bot-rule "When" clause editor. Each
 * element is rendered by {@code renderer} and created/edited by an
 * {@link ItemEditor} (often a nested {@link Form}).
 *
 * @param <T> the element type
 */
public class ListField<T> implements FormField {

    private static final String ADD_ID = "__add__";
    private static final String ITEM_PREFIX = "item:";

    private final String id;
    private final String label;
    private final Function<T, String> renderer;
    private final ItemEditor<T> editor;
    private final List<T> values = new ArrayList<>();
    private boolean required;
    private String error;

    public ListField(String id, String label, Function<T, String> renderer, ItemEditor<T> editor) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    public ListField<T> required() {
        this.required = true;
        return this;
    }

    public List<T> values() {
        return List.copyOf(values);
    }

    public ListField<T> values(List<T> initial) {
        values.clear();
        if (initial != null) {
            values.addAll(initial);
        }
        return this;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String displayValue() {
        return values.isEmpty() ? "(none)" : values.size() + " item(s)";
    }

    @Override
    public void edit(MenuContext context) throws Exception {
        while (true) {
            List<MenuItem> items = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                items.add(MenuItem.action(ITEM_PREFIX + i, renderer.apply(values.get(i)), ctx -> {
                }));
            }
            items.add(MenuItem.action(ADD_ID, "+ Add", OptionColor.GREEN, ctx -> {
            }));

            String selected = context.select(label, items);
            if (selected == null) {
                break; // done editing the list
            }
            if (ADD_ID.equals(selected)) {
                T created = editor.edit(context, null);
                if (created != null) {
                    values.add(created);
                }
            } else if (selected.startsWith(ITEM_PREFIX)) {
                editExisting(context, Integer.parseInt(selected.substring(ITEM_PREFIX.length())));
            }
        }
        revalidate();
    }

    private void editExisting(MenuContext context, int index) throws Exception {
        if (index < 0 || index >= values.size()) {
            return;
        }
        String action = context.select(renderer.apply(values.get(index)), List.of(
                MenuItem.action("edit", "Edit", ctx -> {
                }),
                MenuItem.action("delete", "Delete", OptionColor.RED, ctx -> {
                })));
        if (action == null) {
            return;
        }
        if ("edit".equals(action)) {
            T edited = editor.edit(context, values.get(index));
            if (edited != null) {
                values.set(index, edited);
            }
        } else if ("delete".equals(action)) {
            values.remove(index);
        }
    }

    @Override
    public void revalidate() {
        error = required && values.isEmpty() ? "at least one required" : null;
    }

    @Override
    public String error() {
        return error;
    }
}
