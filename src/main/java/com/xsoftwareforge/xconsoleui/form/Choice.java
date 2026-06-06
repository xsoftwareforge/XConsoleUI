package com.xsoftwareforge.xconsoleui.form;

import java.util.Objects;

/** A selectable option (id + label) for {@link ChoiceField}/{@link MultiChoiceField}. */
public record Choice(String id, String label) {

    public Choice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
    }

    public static Choice of(String id, String label) {
        return new Choice(id, label);
    }
}
