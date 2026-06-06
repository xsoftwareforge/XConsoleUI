package com.xsoftwareforge.xconsoleui.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.form.StubMenuContext;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;
import com.xsoftwareforge.xconsoleui.menu.Menu;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.MenuRegistry;

/**
 * Drives the built-in {@link SettingsMenu} flows through a scripted
 * {@link StubMenuContext}: hotkey rebinding (including collision detection) and
 * the display toggles. These guard the previously untested interactive logic.
 */
public class SettingsMenuTest {

    /** A {@link MenuConfigStore} that keeps the config in memory and counts saves. */
    private static final class CountingStore implements MenuConfigStore {
        private MenuConfig config = MenuConfig.defaults();
        int saves;

        @Override
        public MenuConfig load() {
            return config;
        }

        @Override
        public void save(MenuConfig config) {
            this.config = config;
            saves++;
        }
    }

    private static MenuItem findItem(List<MenuItem> items, String id) {
        for (MenuItem item : items) {
            if (item.id().equals(id)) {
                return item;
            }
        }
        throw new AssertionError("no item with id " + id);
    }

    private static Menu submenu(Menu root, StubMenuContext ctx, String id) {
        return findItem(root.items(ctx), id).submenu();
    }

    @Test
    public void rebindingToFreeKeyAssignsAndPersists() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        HotkeyOption help = HotkeyOption.of("help", "Help", FunctionKey.F1, x -> {
        });
        registry.addHotkeyOption(help);
        MenuConfig config = MenuConfig.defaults();
        CountingStore store = new CountingStore();
        Menu root = SettingsMenu.create(registry, config, store);

        StubMenuContext ctx = new StubMenuContext().queueSelect("F5");
        Menu hotkeys = submenu(root, ctx, "hotkeys");
        findItem(hotkeys.items(ctx), "help").action().run(ctx);

        assertEquals(FunctionKey.F5, config.assignments().get("help"));
        assertEquals(FunctionKey.F5, config.effectiveKey(help));
        assertEquals(1, store.saves);
    }

    @Test
    public void rebindingToKeyUsedByAnotherOptionIsRejected() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        HotkeyOption help = HotkeyOption.of("help", "Help", FunctionKey.F1, x -> {
        });
        HotkeyOption close = HotkeyOption.of("close", "Close", FunctionKey.F2, x -> {
        });
        registry.addHotkeyOption(help);
        registry.addHotkeyOption(close);
        MenuConfig config = MenuConfig.defaults();
        CountingStore store = new CountingStore();
        Menu root = SettingsMenu.create(registry, config, store);

        // Try to put "close" onto F1, which "help" already occupies by default.
        StubMenuContext ctx = new StubMenuContext().queueSelect("F1");
        Menu hotkeys = submenu(root, ctx, "hotkeys");
        findItem(hotkeys.items(ctx), "close").action().run(ctx);

        // Rejected: no assignment recorded, key unchanged, nothing persisted.
        assertNull(config.assignments().get("close"));
        assertEquals(FunctionKey.F2, config.effectiveKey(close));
        assertEquals(0, store.saves);
    }

    @Test
    public void displayMenuTogglesStickyHeaderAndPersists() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        MenuConfig config = MenuConfig.defaults();
        boolean before = config.tableHeaderSticky();
        CountingStore store = new CountingStore();
        Menu root = SettingsMenu.create(registry, config, store);

        StubMenuContext ctx = new StubMenuContext();
        Menu display = submenu(root, ctx, "display");
        findItem(display.items(ctx), "stickyHeader").action().run(ctx);

        assertEquals(!before, config.tableHeaderSticky());
        assertEquals(1, store.saves);
    }

    @Test
    public void displayMenuSetsListViewModeFromSelection() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        MenuConfig config = MenuConfig.defaults();
        CountingStore store = new CountingStore();
        Menu root = SettingsMenu.create(registry, config, store);

        StubMenuContext ctx = new StubMenuContext().queueSelect(ListViewMode.GRID.name());
        Menu display = submenu(root, ctx, "display");
        findItem(display.items(ctx), "listView").action().run(ctx);

        assertEquals(ListViewMode.GRID, config.listViewMode());
        assertTrue(store.saves >= 1);
    }

    @Test
    public void cancellingRebindLeavesConfigUntouched() throws Exception {
        MenuRegistry registry = new MenuRegistry();
        HotkeyOption help = HotkeyOption.of("help", "Help", FunctionKey.F1, x -> {
        });
        registry.addHotkeyOption(help);
        MenuConfig config = MenuConfig.defaults();
        CountingStore store = new CountingStore();
        Menu root = SettingsMenu.create(registry, config, store);

        // Empty queue -> select() returns null -> the rebind is cancelled.
        StubMenuContext ctx = new StubMenuContext();
        Menu hotkeys = submenu(root, ctx, "hotkeys");
        findItem(hotkeys.items(ctx), "help").action().run(ctx);

        assertFalse(config.assignments().containsKey("help"));
        assertEquals(0, store.saves);
    }
}
