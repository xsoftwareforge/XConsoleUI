package com.xsoftwareforge.xconsoleui.menu;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.form.StubMenuContext;

public class CommandPaletteTest {

    @Test
    public void flattensLeafActionsAndRunsTheChosenOne() throws Exception {
        List<String> log = new ArrayList<>();

        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(Menu.builder("m", "M")
                .action("a1", "A1", ctx -> log.add("a1"))
                .submenu("s", "S", sub -> sub.action("a2", "A2", ctx -> log.add("a2")))
                .build());

        // Entries are: [0] "M › A1", [1] "M › S › A2". Pick index 1.
        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.select(1));

        CommandPalette.open(ctx, registry);

        assertEquals(List.of("a2"), log);
    }

    @Test
    public void runsTopLevelActionWhenSelected() throws Exception {
        List<String> log = new ArrayList<>();
        MenuRegistry registry = new MenuRegistry();
        registry.addMenu(Menu.builder("m", "M")
                .action("a1", "A1", ctx -> log.add("a1"))
                .build());

        StubMenuContext ctx = new StubMenuContext().queueBrowse(BrowseResult.select(0));
        CommandPalette.open(ctx, registry);

        assertEquals(List.of("a1"), log);
    }

    @Test
    public void emptyRegistryDoesNotCrash() throws Exception {
        CommandPalette.open(new StubMenuContext(), new MenuRegistry());
        // no exception = pass
    }
}
