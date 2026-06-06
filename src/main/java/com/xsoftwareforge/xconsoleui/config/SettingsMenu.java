package com.xsoftwareforge.xconsoleui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.util.HashSet;
import java.util.Set;

import com.xsoftwareforge.xconsoleui.config.MenuTheme.Slot;
import com.xsoftwareforge.xconsoleui.menu.AbstractMenu;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;
import com.xsoftwareforge.xconsoleui.menu.Menu;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.MenuRegistry;

/**
 * Built-in settings menu that lets the user change theme colors and rebind global
 * hotkeys at runtime. Every change is written back through the supplied
 * {@link MenuConfigStore} immediately.
 * <p>
 * The host adds the returned {@link Menu} to its {@link MenuRegistry}; the same
 * {@link MenuConfig} instance must be the one passed to the running
 * {@code MenuSession} so edits take effect live.
 */
public final class SettingsMenu {

    private static final String RESET_ID = "__reset__";

    private SettingsMenu() {
    }

    public static Menu create(MenuRegistry registry, MenuConfig config, MenuConfigStore store) {
        return create(registry, config, store, "settings", "Settings");
    }

    public static Menu create(MenuRegistry registry, MenuConfig config, MenuConfigStore store, String id,
            String title) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(store, "store");
        return new RootSettingsMenu(registry, config, store, id, title);
    }

    private static final class RootSettingsMenu extends AbstractMenu {
        private final ColorsMenu colorsMenu;
        private final HotkeysMenu hotkeysMenu;
        private final DisplayMenu displayMenu;

        private RootSettingsMenu(MenuRegistry registry, MenuConfig config, MenuConfigStore store, String id,
                String title) {
            super(id, title);
            this.colorsMenu = new ColorsMenu(config, store);
            this.hotkeysMenu = new HotkeysMenu(registry, config, store);
            this.displayMenu = new DisplayMenu(config, store);
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(
                    submenu("colors", "Colors", colorsMenu),
                    submenu("hotkeys", "Hotkeys", hotkeysMenu),
                    submenu("display", "Display", displayMenu));
        }
    }

    private static final class DisplayMenu extends AbstractMenu {
        private final MenuConfig config;
        private final MenuConfigStore store;

        private DisplayMenu(MenuConfig config, MenuConfigStore store) {
            super("display", "Display");
            this.config = config;
            this.store = store;
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(
                    MenuItem.action("listView", "List view: " + config.listViewMode(), ctx -> {
                        List<MenuItem> choices = new ArrayList<>();
                        for (ListViewMode mode : ListViewMode.values()) {
                            choices.add(MenuItem.action(mode.name(),
                                    mode + (mode == config.listViewMode() ? "  (current)" : ""), x -> {
                                    }));
                        }
                        String id = ctx.select("List view mode", choices);
                        if (id != null) {
                            config.setListViewMode(ListViewMode.valueOf(id));
                            store.save(config);
                            ctx.printSuccess("List view set to " + id + ".");
                        }
                    }),
                    MenuItem.action("stickyHeader",
                            "Sticky table header: " + (config.tableHeaderSticky() ? "on" : "off"), ctx -> {
                                config.setTableHeaderSticky(!config.tableHeaderSticky());
                                store.save(config);
                                ctx.printSuccess("Sticky table header "
                                        + (config.tableHeaderSticky() ? "on" : "off") + ".");
                            }));
        }
    }

    private static final class ColorsMenu extends AbstractMenu {
        private final MenuConfig config;
        private final MenuConfigStore store;

        private ColorsMenu(MenuConfig config, MenuConfigStore store) {
            super("colors", "Colors");
            this.config = config;
            this.store = store;
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            List<MenuItem> items = new ArrayList<>();
            for (Slot slot : Slot.values()) {
                MenuColor current = config.theme().color(slot);
                items.add(MenuItem.action(slot.name(), humanize(slot.name()) + ": " + current, ctx -> {
                    MenuColor chosen = pickColor(ctx, current);
                    if (chosen != null) {
                        config.setTheme(config.theme().with(slot, chosen));
                        store.save(config);
                        ctx.printSuccess(humanize(slot.name()) + " set to " + chosen + ".");
                    }
                }));
            }
            return items;
        }

        private static MenuColor pickColor(MenuContext ctx, MenuColor current) throws Exception {
            List<MenuItem> options = new ArrayList<>();
            for (MenuColor color : MenuColor.values()) {
                options.add(MenuItem.action(color.name(), color.name() + (color == current ? "  (current)" : ""),
                        x -> {
                        }));
            }
            String id = ctx.select("Choose a color", options);
            return id == null ? null : MenuColor.valueOf(id);
        }
    }

    private static final class HotkeysMenu extends AbstractMenu {
        private static final String UNBIND_ID = "__unbind__";

        private final MenuRegistry registry;
        private final MenuConfig config;
        private final MenuConfigStore store;

        private HotkeysMenu(MenuRegistry registry, MenuConfig config, MenuConfigStore store) {
            super("hotkeys", "Hotkeys");
            this.registry = registry;
            this.config = config;
            this.store = store;
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            List<MenuItem> items = new ArrayList<>();
            for (HotkeyOption option : registry.hotkeyOptions()) {
                FunctionKey effective = config.effectiveKey(option);
                String current = effective == null ? "(unbound)" : effective.toString();
                String suffix = describeDefault(option, effective);
                items.add(MenuItem.action(option.id(), option.label() + ": " + current + suffix,
                        ctx -> rebind(ctx, option)));
            }
            return items;
        }

        private static String describeDefault(HotkeyOption option, FunctionKey effective) {
            if (option.defaultKey() == effective) {
                return "";
            }
            return "  (default " + (option.defaultKey() == null ? "none" : option.defaultKey()) + ")";
        }

        private void rebind(MenuContext ctx, HotkeyOption option) throws Exception {
            FunctionKey effective = config.effectiveKey(option);
            List<MenuItem> choices = new ArrayList<>();
            choices.add(MenuItem.action(RESET_ID,
                    "(reset to default " + (option.defaultKey() == null ? "none" : option.defaultKey()) + ")", x -> {
                    }));
            choices.add(MenuItem.action(UNBIND_ID, "(unbind / disable)", x -> {
            }));
            for (FunctionKey key : FunctionKey.values()) {
                choices.add(MenuItem.action(key.name(), key.name() + (key == effective ? "  (current)" : ""), x -> {
                }));
            }

            String id = ctx.select("Assign a key for \"" + option.label() + "\"", choices);
            if (id == null) {
                return;
            }
            if (RESET_ID.equals(id)) {
                config.resetHotkey(option.id());
                store.save(config);
                ctx.printSuccess("\"" + option.label() + "\" reset to default.");
                return;
            }
            if (UNBIND_ID.equals(id)) {
                config.unbindHotkey(option.id());
                store.save(config);
                ctx.printSuccess("\"" + option.label() + "\" disabled.");
                return;
            }

            FunctionKey chosen = FunctionKey.valueOf(id);
            String conflict = conflictingLabel(option, chosen);
            if (conflict != null) {
                ctx.printError(chosen + " is already used by \"" + conflict + "\".");
                return;
            }
            config.assignHotkey(option.id(), chosen);
            store.save(config);
            ctx.printSuccess("\"" + option.label() + "\" is now on " + chosen + ".");
        }

        /** @return the label of another option already bound to {@code key}, or null. */
        private String conflictingLabel(HotkeyOption edited, FunctionKey key) {
            Set<String> seen = new HashSet<>();
            for (HotkeyOption option : registry.hotkeyOptions()) {
                if (option.id().equals(edited.id()) || !seen.add(option.id())) {
                    continue;
                }
                if (Objects.equals(config.effectiveKey(option), key)) {
                    return option.label();
                }
            }
            return null;
        }
    }

    private static String humanize(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
