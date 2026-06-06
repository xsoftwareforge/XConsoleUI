package com.xsoftwareforge.xconsoleui.form;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/**
 * One editable field of a {@link Form}. The {@link FormController} renders each
 * field as a {@code label: value} row; pressing Enter calls {@link #edit} which
 * opens the appropriate editor (text input, picker, toggle, ...) via the
 * {@link MenuContext} and mutates the field's value.
 */
public interface FormField {

    String id();

    String label();

    /** The current value rendered into the form row. */
    String displayValue();

    /** Open this field's editor and apply the change. */
    void edit(MenuContext context) throws Exception;

    /** Re-check validity (e.g. on submit); implementations set {@link #error()}. */
    default void revalidate() {
    }

    /** @return a validation error to show, or {@code null} when valid. */
    default String error() {
        return null;
    }

    default boolean isValid() {
        return error() == null;
    }
}
