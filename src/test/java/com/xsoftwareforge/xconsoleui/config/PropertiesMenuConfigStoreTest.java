package com.xsoftwareforge.xconsoleui.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xsoftwareforge.xconsoleui.config.MenuTheme.Slot;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;

public class PropertiesMenuConfigStoreTest {

    private Path file;

    @Before
    public void setUp() throws IOException {
        file = Files.createTempFile("console-menu-test", ".properties");
        Files.delete(file); // start from "not yet saved"
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(file);
    }

    @Test
    public void loadReturnsDefaultsWhenFileMissing() {
        MenuConfig config = new PropertiesMenuConfigStore(file).load();
        assertEquals(MenuColor.CYAN, config.theme().color(Slot.TITLE));
        assertEquals(ListViewMode.LIST, config.listViewMode());
        assertTrue(config.assignments().isEmpty());
        assertTrue(config.unboundOptionIds().isEmpty());
        assertFalse(Files.exists(file));
    }

    @Test
    public void savesAndReloadsThemeAndHotkeyAssignments() {
        PropertiesMenuConfigStore store = new PropertiesMenuConfigStore(file);

        MenuConfig config = MenuConfig.defaults();
        config.setTheme(config.theme().with(Slot.TITLE, MenuColor.RED).with(Slot.OPTION, MenuColor.MAGENTA));
        config.assignHotkey("builtin.help", FunctionKey.F3);
        config.unbindHotkey("builtin.close");
        store.save(config);

        assertTrue(Files.exists(file));

        MenuConfig reloaded = store.load();
        assertEquals(MenuColor.RED, reloaded.theme().color(Slot.TITLE));
        assertEquals(MenuColor.MAGENTA, reloaded.theme().color(Slot.OPTION));
        assertEquals(MenuColor.YELLOW, reloaded.theme().color(Slot.HINT)); // untouched default
        assertEquals(FunctionKey.F3, reloaded.assignments().get("builtin.help"));
        assertTrue(reloaded.unboundOptionIds().contains("builtin.close"));
    }

    @Test
    public void malformedEntriesFallBackToDefaults() throws IOException {
        Files.writeString(file, "theme.TITLE=NOPE\noption.builtin.help=NOPE\ntheme.HINT=red\n");

        MenuConfig config = new PropertiesMenuConfigStore(file).load();
        assertEquals(MenuColor.CYAN, config.theme().color(Slot.TITLE)); // bad value ignored
        assertEquals(MenuColor.RED, config.theme().color(Slot.HINT));   // case-insensitive parse
        assertTrue(config.assignments().isEmpty());                     // bad key ignored
    }
}
