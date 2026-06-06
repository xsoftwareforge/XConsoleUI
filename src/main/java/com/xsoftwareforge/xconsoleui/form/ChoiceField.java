package com.xsoftwareforge.xconsoleui.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;

/** A single-choice field rendered as a picker. */
public class ChoiceField implements FormField {

    private final String id;
    private final String label;
    private final List<Choice> choices;
    private String selectedId;
    private boolean required;
    private String error;

    public ChoiceField(String id, String label, List<Choice> choices) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
    }

    public ChoiceField required() {
        this.required = true;
        return this;
    }

    public ChoiceField select(String choiceId) {
        this.selectedId = choiceId;
        return this;
    }

    public String selectedId() {
        return selectedId;
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
        return labelFor(selectedId);
    }

    @Override
    public void edit(MenuContext context) throws Exception {
        List<MenuItem> options = new ArrayList<>();
        for (Choice choice : choices) {
            options.add(MenuItem.action(choice.id(), choice.label(), ctx -> {
            }));
        }
        String chosen = context.select(label, options);
        if (chosen != null) {
            selectedId = chosen;
            revalidate();
        }
    }

    @Override
    public void revalidate() {
        error = required && (selectedId == null || selectedId.isBlank()) ? "required" : null;
    }

    @Override
    public String error() {
        return error;
    }

    private String labelFor(String choiceId) {
        if (choiceId == null) {
            return "(none)";
        }
        for (Choice choice : choices) {
            if (choice.id().equals(choiceId)) {
                return choice.label();
            }
        }
        return choiceId;
    }
}
