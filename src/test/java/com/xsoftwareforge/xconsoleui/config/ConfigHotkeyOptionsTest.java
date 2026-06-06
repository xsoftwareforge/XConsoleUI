package com.xsoftwareforge.xconsoleui.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.form.StubMenuContext;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;

public class ConfigHotkeyOptionsTest {

    @Test
    public void listViewToggleFlipsAndPersists() throws Exception {
        MenuConfig config = MenuConfig.defaults(); // LIST
        MenuConfigStore store = MenuConfigStore.inMemory();
        HotkeyOption option = ConfigHotkeyOptions.listViewToggle(config, store);

        option.action().run(new StubMenuContext());
        assertEquals(ListViewMode.GRID, config.listViewMode());
        assertEquals(ListViewMode.GRID, store.load().listViewMode());

        option.action().run(new StubMenuContext());
        assertEquals(ListViewMode.LIST, config.listViewMode());
    }

    @Test
    public void stickyHeaderToggleFlips() throws Exception {
        MenuConfig config = MenuConfig.defaults(); // sticky = true
        HotkeyOption option = ConfigHotkeyOptions.stickyHeaderToggle(config, MenuConfigStore.inMemory());

        option.action().run(new StubMenuContext());
        assertEquals(false, config.tableHeaderSticky());
    }
}
