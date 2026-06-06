package com.xsoftwareforge.xconsoleui.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;

public class MenuConfigTest {

    private static final HotkeyOption WITH_DEFAULT =
            HotkeyOption.of("help", "Help", FunctionKey.F1, ctx -> {
            });
    private static final HotkeyOption UNBOUND_BY_DEFAULT =
            HotkeyOption.of("close", "Close", ctx -> {
            });

    @Test
    public void usesDefaultKeyWhenNoOverride() {
        MenuConfig config = MenuConfig.defaults();
        assertEquals(FunctionKey.F1, config.effectiveKey(WITH_DEFAULT));
        assertNull(config.effectiveKey(UNBOUND_BY_DEFAULT));
    }

    @Test
    public void assignOverridesDefault() {
        MenuConfig config = MenuConfig.defaults();
        config.assignHotkey("help", FunctionKey.F4);
        assertEquals(FunctionKey.F4, config.effectiveKey(WITH_DEFAULT));

        config.assignHotkey("close", FunctionKey.F6);
        assertEquals(FunctionKey.F6, config.effectiveKey(UNBOUND_BY_DEFAULT));
    }

    @Test
    public void unbindDisablesEvenADefaultedOption() {
        MenuConfig config = MenuConfig.defaults();
        config.unbindHotkey("help");
        assertNull(config.effectiveKey(WITH_DEFAULT));
    }

    @Test
    public void resetRestoresDefault() {
        MenuConfig config = MenuConfig.defaults();
        config.assignHotkey("help", FunctionKey.F4);
        config.unbindHotkey("close");

        config.resetHotkey("help");
        config.resetHotkey("close");

        assertEquals(FunctionKey.F1, config.effectiveKey(WITH_DEFAULT));
        assertNull(config.effectiveKey(UNBOUND_BY_DEFAULT));
    }
}
