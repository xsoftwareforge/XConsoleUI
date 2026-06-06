package com.xsoftwareforge.xconsoleui.form;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;

/**
 * Creates or edits a single element of a {@link ListField}. Called with the
 * current element ({@code null} when adding a new one); returns the resulting
 * element, or {@code null} to cancel. Implementations typically open a nested
 * {@link Form} or a few {@link MenuContext} prompts.
 *
 * @param <T> the element type
 */
@FunctionalInterface
public interface ItemEditor<T> {

    T edit(MenuContext context, T current) throws Exception;
}
