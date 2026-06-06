package com.xsoftwareforge.xconsoleui.form;

import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/** An integer field with optional range and "required" validation. */
public class IntField implements FormField {

    private final String id;
    private final String label;
    private Integer value;
    private Integer min;
    private Integer max;
    private boolean required;
    private String error;

    public IntField(String id, String label) {
        this(id, label, null);
    }

    public IntField(String id, String label, Integer initialValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.value = initialValue;
    }

    public IntField required() {
        this.required = true;
        return this;
    }

    public IntField range(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }
        this.min = min;
        this.max = max;
        return this;
    }

    public Integer value() {
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
        return value == null ? "(empty)" : String.valueOf(value);
    }

    @Override
    public void edit(MenuContext context) throws Exception {
        String input = context.input(label, value == null ? "" : String.valueOf(value));
        if (input == null) {
            return;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            value = null;
            revalidate();
            return;
        }
        try {
            apply(Integer.parseInt(trimmed));
        } catch (NumberFormatException ex) {
            error = "not a number";
        }
    }

    private void apply(int parsed) {
        if (min != null && parsed < min) {
            error = "min " + min;
            return;
        }
        if (max != null && parsed > max) {
            error = "max " + max;
            return;
        }
        value = parsed;
        error = null;
    }

    @Override
    public void revalidate() {
        if (value == null) {
            error = required ? "required" : null;
            return;
        }
        apply(value);
    }

    @Override
    public String error() {
        return error;
    }
}
