package com.xsoftwareforge.xconsoleui.form;

import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/** A free-text field with optional "required" and custom validation. */
public class TextField implements FormField {

    private final String id;
    private final String label;
    private String value;
    private boolean required;
    private Validator validator;
    private String error;

    public TextField(String id, String label) {
        this(id, label, "");
    }

    public TextField(String id, String label, String initialValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.value = initialValue == null ? "" : initialValue;
    }

    public TextField required() {
        this.required = true;
        return this;
    }

    public TextField validator(Validator validator) {
        this.validator = validator;
        return this;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        revalidate();
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
        return value.isEmpty() ? "(empty)" : value;
    }

    @Override
    public void edit(MenuContext context) throws Exception {
        String input = context.input(label, value);
        if (input != null) {
            setValue(input);
        }
    }

    @Override
    public void revalidate() {
        if (required && value.isBlank()) {
            error = "required";
            return;
        }
        error = validator == null ? null : validator.validate(value);
    }

    @Override
    public String error() {
        return error;
    }
}
