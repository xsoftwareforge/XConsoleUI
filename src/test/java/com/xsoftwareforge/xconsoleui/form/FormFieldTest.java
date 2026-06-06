package com.xsoftwareforge.xconsoleui.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class FormFieldTest {

    @Test
    public void textFieldRequiredFlagsEmpty() throws Exception {
        TextField field = new TextField("name", "Name").required();
        field.revalidate();
        assertEquals("required", field.error());

        StubMenuContext ctx = new StubMenuContext().queueInput("Earth");
        field.edit(ctx);
        assertEquals("Earth", field.value());
        assertTrue(field.isValid());
    }

    @Test
    public void intFieldRejectsNonNumberAndEnforcesRange() throws Exception {
        IntField field = new IntField("priority", "Priority").range(0, 100);

        field.edit(new StubMenuContext().queueInput("abc"));
        assertEquals("not a number", field.error());

        field.edit(new StubMenuContext().queueInput("250"));
        assertEquals("max 100", field.error());

        field.edit(new StubMenuContext().queueInput("42"));
        assertEquals(Integer.valueOf(42), field.value());
        assertTrue(field.isValid());
    }

    @Test
    public void intFieldRejectsReversedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new IntField("priority", "Priority").range(100, 0));
    }

    @Test
    public void toggleFieldFlipsOnEdit() throws Exception {
        ToggleField field = new ToggleField("enabled", "Enabled", false);
        field.edit(new StubMenuContext());
        assertTrue(field.value());
        assertEquals("[on]", field.displayValue());
    }

    @Test
    public void choiceFieldStoresSelection() throws Exception {
        ChoiceField field = new ChoiceField("scope", "Scope",
                List.of(Choice.of("a", "Alpha"), Choice.of("b", "Beta"))).required();
        assertEquals("(none)", field.displayValue());

        field.edit(new StubMenuContext().queueSelect("b"));
        assertEquals("b", field.selectedId());
        assertEquals("Beta", field.displayValue());
        assertNull(field.error());
    }

    @Test
    public void choiceFieldRequiredFlagsNoSelection() {
        ChoiceField field = new ChoiceField("scope", "Scope", List.of(Choice.of("a", "Alpha"))).required();
        field.revalidate();
        assertFalse(field.isValid());
    }
}
