package com.xsoftwareforge.xconsoleui.form;

import java.util.Objects;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/** A boolean field; pressing Enter flips it. */
public class ToggleField implements FormField {

    private final String id;
    private final String label;
    private boolean value;

    public ToggleField(String id, String label) {
        this(id, label, false);
    }

    public ToggleField(String id, String label, boolean initialValue) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.value = initialValue;
    }

    public boolean value() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
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
        return value ? "[on]" : "[off]";
    }

    @Override
    public void edit(MenuContext context) {
        value = !value;
    }
}
