package com.xsoftwareforge.xconsoleui.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Fluent builder for a {@link Form}. Each {@code add}/typed helper returns the
 * created field so the caller can keep a reference and read its value after the
 * form is submitted:
 *
 * <pre>{@code
 * FormBuilder fb = Form.builder("Create rule");
 * TextField id = fb.text("id", "ID").required();
 * IntField prio = fb.intField("priority", "Priority").range(0, 1000);
 * if (FormController.open(ctx, fb.build())) {
 *     save(id.value(), prio.value());
 * }
 * }</pre>
 */
public final class FormBuilder {

    private final String title;
    private final List<FormField> fields = new ArrayList<>();

    FormBuilder(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public <F extends FormField> F add(F field) {
        fields.add(Objects.requireNonNull(field, "field"));
        return field;
    }

    public TextField text(String id, String label) {
        return add(new TextField(id, label));
    }

    public IntField intField(String id, String label) {
        return add(new IntField(id, label));
    }

    public MaskedField masked(String id, String label) {
        return add(new MaskedField(id, label));
    }

    public ToggleField toggle(String id, String label) {
        return add(new ToggleField(id, label));
    }

    public ChoiceField choice(String id, String label, List<Choice> choices) {
        return add(new ChoiceField(id, label, choices));
    }

    public MultiChoiceField multiChoice(String id, String label, List<Choice> choices) {
        return add(new MultiChoiceField(id, label, choices));
    }

    public <T> ListField<T> list(String id, String label, Function<T, String> renderer, ItemEditor<T> editor) {
        return add(new ListField<>(id, label, renderer, editor));
    }

    public Form build() {
        return new Form(title, fields);
    }
}
