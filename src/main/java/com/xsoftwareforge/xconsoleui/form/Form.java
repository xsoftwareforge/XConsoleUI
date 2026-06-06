package com.xsoftwareforge.xconsoleui.form;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An ordered set of {@link FormField}s shown on one screen and edited field by
 * field. Build one with {@link #builder(String)} and run it with
 * {@link FormController#open(com.xsoftwareforge.xconsoleui.menu.MenuContext, Form)}.
 */
public final class Form {

    private final String title;
    private final List<FormField> fields;

    Form(String title, List<FormField> fields) {
        this.title = Objects.requireNonNull(title, "title");
        this.fields = List.copyOf(fields);
    }

    public static FormBuilder builder(String title) {
        return new FormBuilder(title);
    }

    public String title() {
        return title;
    }

    public List<FormField> fields() {
        return fields;
    }

    /** @return the field with the given id, or {@link Optional#empty()} if none matches. */
    public Optional<FormField> field(String id) {
        for (FormField field : fields) {
            if (field.id().equals(id)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }
}
