package com.xsoftwareforge.xconsoleui.form;

import java.util.ArrayList;
import java.util.List;

import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.OptionColor;

/**
 * Drives a {@link Form} using only {@link MenuContext} primitives, so it runs on
 * any terminal screen and is unit-testable with a scripted context. Renders the
 * fields as {@code label: value} rows; Enter edits the focused field, "Save"
 * validates and submits, Esc cancels.
 */
public final class FormController {

    private static final String SAVE_ID = "__save__";

    private FormController() {
    }

    /**
     * Open {@code form} and loop until the user saves (all fields valid) or
     * cancels.
     *
     * @return {@code true} if submitted, {@code false} if cancelled
     */
    public static boolean open(MenuContext context, Form form) throws Exception {
        while (true) {
            List<MenuItem> items = new ArrayList<>();
            for (FormField field : form.fields()) {
                OptionColor color = field.error() != null ? OptionColor.RED : OptionColor.DEFAULT;
                items.add(MenuItem.action(field.id(), row(field), color, ctx -> {
                }));
            }
            items.add(MenuItem.action(SAVE_ID, "Save", OptionColor.GREEN, ctx -> {
            }));

            String selected = context.select(form.title(), items);
            if (selected == null) {
                return false; // Esc / q cancels
            }
            if (SAVE_ID.equals(selected)) {
                if (validate(form)) {
                    return true;
                }
                continue; // invalid fields now show their error in the rows
            }
            FormField field = form.field(selected).orElse(null);
            if (field != null) {
                field.edit(context);
                field.revalidate();
            }
        }
    }

    private static boolean validate(Form form) {
        boolean ok = true;
        for (FormField field : form.fields()) {
            field.revalidate();
            if (!field.isValid()) {
                ok = false;
            }
        }
        return ok;
    }

    private static String row(FormField field) {
        String base = field.label() + ": " + field.displayValue();
        return field.error() == null ? base : base + "  [" + field.error() + "]";
    }
}
