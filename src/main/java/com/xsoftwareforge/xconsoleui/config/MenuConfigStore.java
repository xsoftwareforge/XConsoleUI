package com.xsoftwareforge.xconsoleui.config;

/**
 * Persistence boundary for {@link MenuConfig}. The library never decides where
 * settings live; the host supplies a store (e.g. a
 * {@link PropertiesMenuConfigStore} pointed at its data folder).
 */
public interface MenuConfigStore {

    /** Load the persisted config, or defaults if nothing has been saved yet. */
    MenuConfig load();

    /** Persist the given config. */
    void save(MenuConfig config);

    /** A non-persistent store that just keeps the config in memory. */
    static MenuConfigStore inMemory() {
        return new MenuConfigStore() {
            private MenuConfig config = MenuConfig.defaults();

            @Override
            public MenuConfig load() {
                return config;
            }

            @Override
            public void save(MenuConfig config) {
                this.config = config;
            }
        };
    }
}
