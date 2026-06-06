package com.xsoftwareforge.xconsoleui.menu;

/**
 * Callback executed when a leaf {@link MenuItem} is selected. The action runs on
 * the menu thread and may freely use the supplied {@link MenuContext} to prompt
 * for further input or print buffered output.
 */
@FunctionalInterface
public interface MenuAction {

    void run(MenuContext context) throws Exception;
}
