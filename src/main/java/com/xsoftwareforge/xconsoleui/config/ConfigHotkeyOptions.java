package com.xsoftwareforge.xconsoleui.config;

import java.util.List;

import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;

/**
 * Ready-made {@link HotkeyOption}s that toggle persisted display settings, so the
 * user can flip them with a function key instead of opening the settings menu.
 * They need the live {@link MenuConfig} and a {@link MenuConfigStore}, which is
 * why they live here rather than in {@code BuiltinHotkeyOptions}.
 */
public final class ConfigHotkeyOptions {

    public static final String LIST_VIEW_ID = "config.listView";
    public static final String TABLE_STICKY_ID = "config.tableHeaderSticky";

    private ConfigHotkeyOptions() {
    }

    /** Toggle the list view between LIST (single column) and GRID (multi-column). Defaults to F8. */
    public static HotkeyOption listViewToggle(MenuConfig config, MenuConfigStore store) {
        return new HotkeyOption(LIST_VIEW_ID, "Toggle list view", FunctionKey.F8, ctx -> {
            ListViewMode next = config.listViewMode() == ListViewMode.LIST
                    ? ListViewMode.GRID
                    : ListViewMode.LIST;
            config.setListViewMode(next);
            store.save(config);
            ctx.notify("Display", List.of("List view is now " + next + "."));
        });
    }

    /** Toggle the sticky table header. Unbound by default. */
    public static HotkeyOption stickyHeaderToggle(MenuConfig config, MenuConfigStore store) {
        return new HotkeyOption(TABLE_STICKY_ID, "Toggle sticky header", null, ctx -> {
            config.setTableHeaderSticky(!config.tableHeaderSticky());
            store.save(config);
            ctx.notify("Display", List.of("Sticky table header: " + (config.tableHeaderSticky() ? "on" : "off")));
        });
    }
}
