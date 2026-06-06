package com.xsoftwareforge.xconsoleui.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;

/** A multi-choice field rendered as a checkbox picker. */
public class MultiChoiceField implements FormField {

    private final String id;
    private final String label;
    private final List<Choice> choices;
    private final List<String> selectedIds = new ArrayList<>();

    public MultiChoiceField(String id, String label, List<Choice> choices) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
    }

    public List<String> selectedIds() {
        return List.copyOf(selectedIds);
    }

    public MultiChoiceField select(List<String> ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
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
        if (selectedIds.isEmpty()) {
            return "(none)";
        }
        List<String> labels = new ArrayList<>();
        for (String selected : selectedIds) {
            labels.add(labelFor(selected));
        }
        return String.join(", ", labels);
    }

    @Override
    public void edit(MenuContext context) throws Exception {
        List<MenuItem> options = new ArrayList<>();
        for (Choice choice : choices) {
            options.add(MenuItem.action(choice.id(), choice.label(), ctx -> {
            }));
        }
        List<String> chosen = context.selectMultiple(label, options, selectedIds());
        if (chosen != null) {
            selectedIds.clear();
            selectedIds.addAll(chosen);
        }
    }

    private String labelFor(String choiceId) {
        for (Choice choice : choices) {
            if (choice.id().equals(choiceId)) {
                return choice.label();
            }
        }
        return choiceId;
    }
}
