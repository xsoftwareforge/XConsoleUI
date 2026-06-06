package com.xsoftwareforge.xconsoleui.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ListFieldTest {

    private static ListField<String> field() {
        return new ListField<>("c", "Conditions", s -> s, (ctx, current) -> ctx.input("v", current));
    }

    @Test
    public void addsItems() throws Exception {
        ListField<String> field = field();
        StubMenuContext ctx = new StubMenuContext()
                .queueSelect("__add__").queueInput("a")
                .queueSelect("__add__").queueInput("b");
        // trailing select returns null (empty queue) -> finish

        field.edit(ctx);

        assertEquals(List.of("a", "b"), field.values());
    }

    @Test
    public void deletesSelectedItem() throws Exception {
        ListField<String> field = field();
        StubMenuContext ctx = new StubMenuContext()
                .queueSelect("__add__").queueInput("a")
                .queueSelect("__add__").queueInput("b")
                .queueSelect("item:0").queueSelect("delete");

        field.edit(ctx);

        assertEquals(List.of("b"), field.values());
    }

    @Test
    public void editsSelectedItem() throws Exception {
        ListField<String> field = field();
        StubMenuContext ctx = new StubMenuContext()
                .queueSelect("__add__").queueInput("a")
                .queueSelect("item:0").queueSelect("edit").queueInput("a2");

        field.edit(ctx);

        assertEquals(List.of("a2"), field.values());
    }

    @Test
    public void requiredFlagsEmptyList() {
        ListField<String> field = field().required();
        field.revalidate();
        assertFalse(field.isValid());

        field.values(List.of("x"));
        field.revalidate();
        assertTrue(field.isValid());
    }
}
