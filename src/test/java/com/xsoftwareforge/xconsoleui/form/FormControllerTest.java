package com.xsoftwareforge.xconsoleui.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FormControllerTest {

    @Test
    public void editsFieldsThenSaves() throws Exception {
        FormBuilder fb = Form.builder("Create rule");
        TextField id = fb.text("id", "ID").required();
        IntField priority = fb.intField("priority", "Priority").range(0, 1000);
        Form form = fb.build();

        // Select "id" -> input rule1; select "priority" -> input 50; select Save.
        StubMenuContext ctx = new StubMenuContext()
                .queueSelect("id").queueInput("rule1")
                .queueSelect("priority").queueInput("50")
                .queueSelect("__save__");

        boolean saved = FormController.open(ctx, form);

        assertTrue(saved);
        assertEquals("rule1", id.value());
        assertEquals(Integer.valueOf(50), priority.value());
    }

    @Test
    public void escapeCancels() throws Exception {
        FormBuilder fb = Form.builder("Create rule");
        fb.text("id", "ID");
        Form form = fb.build();

        StubMenuContext ctx = new StubMenuContext(); // empty -> select returns null

        assertFalse(FormController.open(ctx, form));
    }

    @Test
    public void saveBlockedWhileRequiredFieldInvalid() throws Exception {
        FormBuilder fb = Form.builder("Create rule");
        TextField id = fb.text("id", "ID").required();
        Form form = fb.build();

        // Press Save while ID empty (invalid -> loop continues); the next select
        // returns null (empty queue) which cancels.
        StubMenuContext ctx = new StubMenuContext().queueSelect("__save__");

        boolean saved = FormController.open(ctx, form);

        assertFalse(saved);
        assertEquals("required", id.error());
    }
}
