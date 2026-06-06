package com.xsoftwareforge.xconsoleui.form;

/**
 * Validates a text value. Returns an error message to display, or {@code null}
 * when the value is acceptable.
 */
@FunctionalInterface
public interface Validator {

    String validate(String value);
}
