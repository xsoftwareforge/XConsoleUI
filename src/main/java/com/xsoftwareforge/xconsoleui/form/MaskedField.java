package com.xsoftwareforge.xconsoleui.form;

import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/** A masked text field (e.g. a password); the value is hidden in the form row. */
public class MaskedField implements FormField {

    private final String id;
    private final String label;
    private String value = "";
    private boolean required;
    private String error;

    public MaskedField(String id, String label) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
    }

    public MaskedField required() {
        this.required = true;
        return this;
    }

    public String value() {
        return value;
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
        return value.isEmpty() ? "(empty)" : "*".repeat(value.length());
    }

    @Override
    public void edit(MenuContext context) throws Exception {
        String input = context.inputMasked(label);
        if (input != null) {
            value = input;
            revalidate();
        }
    }

    @Override
    public void revalidate() {
        error = required && value.isBlank() ? "required" : null;
    }

    @Override
    public String error() {
        return error;
    }
}
