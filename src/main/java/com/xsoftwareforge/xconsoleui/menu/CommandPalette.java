package com.xsoftwareforge.xconsoleui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A type-ahead command palette: flattens every leaf action across the registered
 * menu tree into a searchable list (path shown as {@code Menu › Sub › Action}),
 * lets the user filter and pick one, then runs it. Submenu entries are descended
 * into; only runnable leaf actions are listed.
 */
public final class CommandPalette {

    private static final int MAX_DEPTH = 6;

    private CommandPalette() {
    }

    public static void open(MenuContext context, MenuRegistry registry) throws Exception {
        List<Entry> entries = new ArrayList<>();
        for (Menu menu : registry.menus()) {
            collect(context, menu, menu.title(), 0, entries);
        }
        if (entries.isEmpty()) {
            context.showResult("Command palette", List.of("(no commands)"));
            return;
        }

        List<String> rows = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            rows.add(entry.label());
        }
        BrowseResult result = context.browse("Command palette", rows, Map.of(), true);
        if (result.kind() == BrowseResult.Kind.SELECT && result.index() >= 0 && result.index() < entries.size()) {
            entries.get(result.index()).action().run(context);
        }
    }

    private static void collect(MenuContext context, Menu menu, String prefix, int depth, List<Entry> out) {
        if (depth > MAX_DEPTH) {
            return;
        }
        List<MenuItem> items = menu.items(context);
        if (items == null) {
            return;
        }
        for (MenuItem item : items) {
            String label = prefix + " › " + item.label();
            if (item.isSubmenu()) {
                collect(context, item.submenu(), label, depth + 1, out);
            } else if (item.action() != null) {
                out.add(new Entry(label, item.action()));
            }
        }
    }

    private record Entry(String label, MenuAction action) {
    }
}
