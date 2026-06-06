package com.xsoftwareforge.xconsoleui.menu;

/**
 * Outcome of an interactive row browser ({@link MenuContext#browse}). The user
 * either selected the highlighted row (Enter), triggered a row hotkey action on
 * it, or went back (Esc).
 */
public final class BrowseResult {

    public enum Kind {
        SELECT,
        ACTION,
        BACK
    }

    private static final BrowseResult BACK = new BrowseResult(Kind.BACK, -1, null);

    private final Kind kind;
    private final int index;
    private final FunctionKey actionKey;

    private BrowseResult(Kind kind, int index, FunctionKey actionKey) {
        this.kind = kind;
        this.index = index;
        this.actionKey = actionKey;
    }

    public static BrowseResult select(int index) {
        return new BrowseResult(Kind.SELECT, index, null);
    }

    public static BrowseResult action(FunctionKey key, int index) {
        return new BrowseResult(Kind.ACTION, index, key);
    }

    public static BrowseResult back() {
        return BACK;
    }

    public Kind kind() {
        return kind;
    }

    /** Index into the rows that were passed to {@code browse}; -1 for {@link Kind#BACK}. */
    public int index() {
        return index;
    }

    public FunctionKey actionKey() {
        return actionKey;
    }

    public boolean isBack() {
        return kind == Kind.BACK;
    }
}
