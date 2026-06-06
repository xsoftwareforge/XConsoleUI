package com.xsoftwareforge.xconsoleui.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Ready-made {@link HotkeyOption}s the library ships so a host can offer common
 * actions without writing them. They are plain options: register the ones you
 * want and let the user (re)assign their keys in the settings editor.
 */
public final class BuiltinHotkeyOptions {

    public static final String HELP_ID = "builtin.help";
    public static final String CLOSE_ID = "builtin.close";
    public static final String CONSOLE_ID = "builtin.console";
    public static final String PALETTE_ID = "builtin.palette";

    private BuiltinHotkeyOptions() {
    }

    /** Show the list of top-level menus. Defaults to F1. */
    public static HotkeyOption help(MenuRegistry registry) {
        return new HotkeyOption(HELP_ID, "Help", FunctionKey.F1, ctx -> {
            List<String> lines = new ArrayList<>();
            for (Menu menu : registry.menus()) {
                lines.add(menu.title());
            }
            ctx.showResult("Menus", lines);
        });
    }

    /** Show the buffered console output (scrollback). Defaults to F12. */
    public static HotkeyOption console() {
        return new HotkeyOption(CONSOLE_ID, "Console", FunctionKey.F12, MenuContext::showConsole);
    }

    /** Open the type-ahead command palette over all menus. Defaults to F4. */
    public static HotkeyOption commandPalette(MenuRegistry registry) {
        return new HotkeyOption(PALETTE_ID, "Command palette", FunctionKey.F4,
                ctx -> CommandPalette.open(ctx, registry));
    }

    /** Close the menu session and return to the host. Unbound by default. */
    public static HotkeyOption closeMenu() {
        return new HotkeyOption(CLOSE_ID, "Close menu", null, MenuContext::close);
    }

    /** All built-in options, ready to register on a {@link MenuRegistry}. */
    public static List<HotkeyOption> all(MenuRegistry registry) {
        return List.of(help(registry), console(), commandPalette(registry), closeMenu());
    }
}
