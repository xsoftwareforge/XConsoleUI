package com.xsoftwareforge.xconsoleui.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;

/**
 * Mutable, persistable settings for the menu system: the color {@link MenuTheme}
 * and the user's hotkey assignments.
 * <p>
 * Hotkey assignments are keyed by a {@link HotkeyOption}'s stable id:
 * <ul>
 *   <li>an explicit <em>assignment</em> binds an option to a chosen key,</li>
 *   <li>an explicit <em>unbind</em> disables an option (no key),</li>
 *   <li>no entry means the option uses its {@link HotkeyOption#defaultKey()}.</li>
 * </ul>
 * Actions are code and are never persisted; only the key assignment is.
 */
public final class MenuConfig {

    private MenuTheme theme;
    private ListViewMode listViewMode = ListViewMode.LIST;
    private boolean tableHeaderSticky = true;
    private FunctionKey viewToggleKey = FunctionKey.F8;
    private final Map<String, FunctionKey> assignments = new LinkedHashMap<>();
    private final Set<String> unbound = new LinkedHashSet<>();

    public MenuConfig() {
        this.theme = MenuTheme.defaults();
    }

    public static MenuConfig defaults() {
        return new MenuConfig();
    }

    public MenuTheme theme() {
        return theme;
    }

    public void setTheme(MenuTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public ListViewMode listViewMode() {
        return listViewMode;
    }

    public void setListViewMode(ListViewMode mode) {
        this.listViewMode = Objects.requireNonNull(mode, "mode");
    }

    public boolean tableHeaderSticky() {
        return tableHeaderSticky;
    }

    public void setTableHeaderSticky(boolean sticky) {
        this.tableHeaderSticky = sticky;
    }

    /** The key that toggles a single list's layout (LIST/GRID); {@code null} disables it. Default F8. */
    public FunctionKey viewToggleKey() {
        return viewToggleKey;
    }

    public void setViewToggleKey(FunctionKey key) {
        this.viewToggleKey = key;
    }

    /** @return the default layout applied to lists that have no explicit override. */
    public ListViewMode defaultListViewMode() {
        return listViewMode;
    }

    /** Resolve the effective key for an option, or {@code null} if unbound. */
    public FunctionKey effectiveKey(HotkeyOption option) {
        Objects.requireNonNull(option, "option");
        if (unbound.contains(option.id())) {
            return null;
        }
        FunctionKey assigned = assignments.get(option.id());
        return assigned != null ? assigned : option.defaultKey();
    }

    /** Bind the option {@code optionId} to {@code key}. */
    public void assignHotkey(String optionId, FunctionKey key) {
        Objects.requireNonNull(optionId, "optionId");
        Objects.requireNonNull(key, "key");
        assignments.put(optionId, key);
        unbound.remove(optionId);
    }

    /** Disable the option {@code optionId} (no key). */
    public void unbindHotkey(String optionId) {
        Objects.requireNonNull(optionId, "optionId");
        assignments.remove(optionId);
        unbound.add(optionId);
    }

    /** Restore the option {@code optionId} to its default key. */
    public void resetHotkey(String optionId) {
        Objects.requireNonNull(optionId, "optionId");
        assignments.remove(optionId);
        unbound.remove(optionId);
    }

    /** @return a snapshot of explicit {@code optionId -> key} assignments. */
    public Map<String, FunctionKey> assignments() {
        return new LinkedHashMap<>(assignments);
    }

    /** @return a snapshot of the option ids that are explicitly unbound. */
    public Set<String> unboundOptionIds() {
        return new LinkedHashSet<>(unbound);
    }
}
