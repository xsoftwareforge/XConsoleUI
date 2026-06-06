package com.xsoftwareforge.xconsoleui.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import com.xsoftwareforge.xconsoleui.config.MenuTheme.Slot;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;

/**
 * {@link MenuConfigStore} backed by a {@code .properties} file. Dependency-free
 * (uses {@link Properties}). Unknown or malformed entries are ignored and fall
 * back to defaults, so a hand-edited or partial file never breaks loading.
 * <p>
 * File layout:
 *
 * <pre>
 *   theme.TITLE=CYAN
 *   theme.SELECTED_BACKGROUND=BLUE
 *   option.builtin.help=F3     # the "help" option is bound to F3
 *   option.builtin.close=NONE  # the "close menu" option is disabled
 * </pre>
 */
public final class PropertiesMenuConfigStore implements MenuConfigStore {

    private static final String THEME_PREFIX = "theme.";
    private static final String OPTION_PREFIX = "option.";
    private static final String UNBOUND_VALUE = "NONE";
    private static final String VIEW_TABLE_STICKY_KEY = "view.tableHeaderSticky";

    private final Path path;

    public PropertiesMenuConfigStore(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    @Override
    public MenuConfig load() {
        MenuConfig config = MenuConfig.defaults();
        if (!Files.exists(path)) {
            return config;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read menu config: " + path, ex);
        }

        MenuTheme theme = config.theme();
        for (Slot slot : Slot.values()) {
            MenuColor color = parseColor(props.getProperty(THEME_PREFIX + slot.name()));
            if (color != null) {
                theme = theme.with(slot, color);
            }
        }
        config.setTheme(theme);

        String sticky = props.getProperty(VIEW_TABLE_STICKY_KEY);
        if (sticky != null) {
            config.setTableHeaderSticky(Boolean.parseBoolean(sticky.trim()));
        }

        for (String name : props.stringPropertyNames()) {
            if (!name.startsWith(OPTION_PREFIX)) {
                continue;
            }
            String optionId = name.substring(OPTION_PREFIX.length());
            if (optionId.isEmpty()) {
                continue;
            }
            String value = props.getProperty(name);
            if (value != null && value.trim().equalsIgnoreCase(UNBOUND_VALUE)) {
                config.unbindHotkey(optionId);
                continue;
            }
            FunctionKey key = parseKey(value);
            if (key != null) {
                config.assignHotkey(optionId, key);
            }
        }
        return config;
    }

    @Override
    public void save(MenuConfig config) {
        Objects.requireNonNull(config, "config");
        Properties props = new Properties();

        MenuTheme theme = config.theme();
        for (Slot slot : Slot.values()) {
            props.setProperty(THEME_PREFIX + slot.name(), theme.color(slot).name());
        }
        props.setProperty(VIEW_TABLE_STICKY_KEY, Boolean.toString(config.tableHeaderSticky()));
        for (Map.Entry<String, FunctionKey> entry : config.assignments().entrySet()) {
            props.setProperty(OPTION_PREFIX + entry.getKey(), entry.getValue().name());
        }
        for (String optionId : config.unboundOptionIds()) {
            props.setProperty(OPTION_PREFIX + optionId, UNBOUND_VALUE);
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "console-menu settings");
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write menu config: " + path, ex);
        }
    }

    private static MenuColor parseColor(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MenuColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static FunctionKey parseKey(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FunctionKey.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
