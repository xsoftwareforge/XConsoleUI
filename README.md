# XConsoleUI

A small, reusable library for **cursor-driven terminal menus** on top of
[JLine](https://github.com/jline/jline3). It provides:

- a **registerable menu model** — every contributor adds its own top-level menu
  (with nested sub-menus and leaf actions) to a shared `MenuRegistry`;
- a **cursor navigation engine** (`TerminalScreen` / `JLineTerminalScreen`):
  arrow keys, Enter, Esc, Page Up/Down, single- and multi-select, free-text and
  masked input, and scrollable result views;
- a **buffered output coordinator** (`ConsoleOutputCoordinator`): asynchronous
  log output is held back while a menu/wizard is on screen and replayed when it
  closes, so background logging never corrupts the rendered UI.

The library has **no dependency on any host** (no plugin system, no game server),
so it can be reused across projects. It targets **JLine 4** and uses its built-in
**JNI terminal provider** (no JNA/Jansi native dependency); requires **Java 17+**.

## Layers

```
com.xsoftwareforge.xconsoleui
 ├─ menu     Menu, AbstractMenu, MenuItem, MenuAction, MenuContext,
 │           OptionColor, MenuRegistry            (pure model, no JLine)
 ├─ output   ConsoleOutputCoordinator, OutputPrinter, SessionState
 ├─ screen   TerminalScreen (interface) + JLineTerminalScreen (engine)
 └─ runner   MenuSession                          (drives navigation = MenuContext)
```

## Usage

Two ways to define a menu.

**Fluent builder** (for static menus — no subclass needed):

```java
Menu menu = Menu.builder("my", "My plugin")
    .action("ping", "Ping", ctx -> ctx.printSuccess("pong"))
    .submenu("tools", "Tools", tools -> tools
        .action("about", "About", ctx -> ctx.print("v1")))
    .build();
```

**Subclass `AbstractMenu`** (for dynamic menus whose items depend on live state):

```java
public final class MyMenu extends AbstractMenu {
    public MyMenu() {
        super("my", "My plugin");
    }

    @Override
    public List<MenuItem> items(MenuContext ctx) {
        return List.of(
            action("ping", "Ping", c -> c.printSuccess("pong")),
            submenu("tools", "Tools", new MyToolsMenu()));
    }
}
```

Navigation keys: arrows + Home/End + Page Up/Down, Enter to confirm, Esc or `q`
to go back (Esc/`q` at the root closes the menu).

## Function-key hotkeys

Active F1-F12 hotkeys are shown in a hint line above the list. There are two
kinds:

**Global hotkey options** — named, *assignable* actions. They have a stable id
and an optional default key, and the user can (re)assign or disable them in the
settings editor (persisted). The library ships a few built-ins.

```java
registry.addHotkeyOptions(BuiltinHotkeyOptions.all(registry));     // Help (F1), Console (F12), Close menu
registry.addHotkeyOption(HotkeyOption.of("reload", "Reload", FunctionKey.F5, ctx -> reload()));
registry.addHotkeyOption(HotkeyOption.of("audit", "Audit", ctx -> audit(ctx))); // starts unbound
```

The built-in **Console** option (`ctx.showConsole()`) opens the buffered output
history in a scrollable view (Page Up/Down to scroll, Esc to return) — handy to
read log lines or hotkey output that was buffered while a menu was on screen.

The built-in **Command palette** option (default F4, `CommandPalette.open(ctx,
registry)`) flattens every leaf action across the whole menu tree into a
type-ahead searchable list (`Menu › Sub › Action`) and runs the chosen one —
jump to any command without clicking through submenus.

**Per-menu hotkeys** — fixed to their key, active only while that menu is on
screen, and they override a global option on the same effective key. Define them
via the builder or by overriding `Menu#hotkeys()`:

```java
Menu.builder("players", "Players")
    .hotkey(FunctionKey.F2, "Ban", ctx -> ban(ctx))
    .build();
```

Any action can call `ctx.close()` to leave the whole menu (that is what the
built-in "Close menu" option does).

Wire it up (typically once per host):

```java
MenuRegistry registry = new MenuRegistry();
registry.addMenu(new MyMenu());          // contributors call this

ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
coordinator.attachPrinter(line -> lineReader.printAbove(line)); // route async logs

// when the user runs the "menu" command:
new MenuSession(terminal, registry, coordinator, "Admin menu").open();
```

## Forms

Compose a data-entry form from reusable input components instead of hand-rolling
a keystroke wizard. Navigate fields with the arrows, Enter edits the focused
field with the right editor, "Save" validates and submits, Esc cancels.

```java
FormBuilder fb = Form.builder("Create rule");
TextField id = fb.text("id", "ID").required();
ChoiceField scope = fb.choice("scope", "Scope",
        List.of(Choice.of("storage", "Storage"), Choice.of("expansion", "Expansion"))).required();
IntField priority = fb.intField("priority", "Priority").range(0, 1000);
ToggleField enabled = fb.toggle("enabled", "Enabled");

if (FormController.open(ctx, fb.build())) {
    save(id.value(), scope.selectedId(), priority.value(), enabled.value());
}
```

Field types: `TextField`, `IntField` (range), `MaskedField`, `ToggleField`,
`ChoiceField`, `MultiChoiceField`, and `ListField<T>` — each with optional
`required()` and inline validation errors. `FormController` runs purely on
`MenuContext` primitives, so it works on any screen and is unit-testable with a
scripted context.

`ListField<T>` is a repeatable list (the generalization of the bot-rule "When"
clause editor): add/edit/delete elements, where each element is created by an
`ItemEditor<T>` — typically a nested `Form`:

```java
fb.list("when", "When (conditions)", c -> c, (ctx, current) -> {
    FormBuilder cf = Form.builder(current == null ? "Add condition" : "Edit condition");
    TextField path = cf.text("path", "Path").required();
    // ... return "path op value" or null to cancel
    return FormController.open(ctx, cf.build()) ? path.value() : null;
}).required();
```

## Selectable lists (RowBrowser)

Browse a list of objects with cursor selection, **type-ahead filtering**, and
**per-row function-key actions** — e.g. an admin "list sockets" where each row
can be kicked or inspected.

```java
RowBrowser.create("Connected sockets", () -> sockets, s -> s.describe())
    .filterable(true)
    .action(FunctionKey.F2, "Kick", (ctx, s) -> { s.kick(); ctx.printSuccess("kicked"); })
    .action(FunctionKey.F3, "Info", (ctx, s) -> ctx.showResult("Info", s.details()))
    .onSelect((ctx, s) -> ctx.showResult("Info", s.details()))
    .open(ctx);

// or as a pure picker:
Optional<Socket> chosen = RowBrowser.create("Pick a socket", sockets, Socket::name).pick(ctx);
```

In the browser: type to filter, arrows/PgUp/PgDn/Home/End to move, Enter selects,
the bound function keys act on the highlighted row, Esc clears the filter (or goes
back). The data is read from a supplier on each redraw, so an action that removes
a row is reflected immediately.

## Tables

Build a table, let the library size the columns, and show it in a scrollable view
with a (configurable) sticky header. Optional export to CSV/JSON.

```java
Table planets = TableBuilder.create()
    .column("Planet")
    .column("Pop", Align.RIGHT)
    .row("Earth", 8000)
    .row("Mars", 120)
    .build();

// inside a menu action:
ctx.showTable("Planets", planets);                              // view only
ctx.showTable("Planets", planets, new FileTableExporter(dir));  // 'e' to export
```

For an **interactive, selectable** table (pick a row, act on it, type-ahead
filter) use `TableBrowser<T>`:

```java
TableBrowser.create("Planets", () -> planets)
    .column(TableColumnDef.of("Planet", Planet::name))
    .column(TableColumnDef.of("Pop", Align.RIGHT, p -> "" + p.pop()))
    .column(TableColumnDef.of("Owner", Planet::owner).editable((ctx, p) -> {   // editable cell
        String v = ctx.input("Owner", p.owner());
        if (v != null) p.setOwner(v);
    }))
    .action(FunctionKey.F2, "Abandon", (ctx, p) -> abandon(p))
    .onSelect((ctx, p) -> ctx.showResult(p.name(), p.details()))
    .open(ctx);
```

Press **Tab** to switch between **row mode** (whole-row selection) and **cell
mode** (Left/Right pick a single cell; Enter edits an `editable` cell inline).
Cell mode is auto-enabled when any column is editable, or force it with
`.cellSelectable(true)`.

In the read-only table view: arrows/PgUp/PgDn/Home/End scroll, `h` toggles the header,
`e` exports (prompts for CSV/JSON/both and a file name; the host's
`TableExporter` decides where files go). Column widths auto-fit header and
content, truncating with `…` only when the whole table exceeds the width. The
sticky-header behavior and inline-vs-fullscreen rendering follow the settings.

## Settings persistence (theme + hotkey rebinding)

Colors and hotkey rebindings live in a `MenuConfig`, persisted through a
`MenuConfigStore`. The library never picks the location — the host supplies it
(e.g. a `.properties` file in its data folder). A built-in `SettingsMenu` lets
the user change colors and reassign function keys at runtime; every change is
saved immediately.

```java
MenuConfigStore store = new PropertiesMenuConfigStore(dataFolder.resolve("console-menu.properties"));
MenuConfig config = store.load();

registry.addMenu(SettingsMenu.create(registry, config, store));   // in-menu editor

new MenuSession(terminal, registry, coordinator, "Admin menu", config).open();
```

Lists can be shown as `LIST` (one entry per line) or `GRID` (multiple columns
filling the width; cursor moves Up/Down sequentially and Left/Right across
columns). **The view is per-list:** press the view-toggle key (default **F8**,
`MenuConfig.viewToggleKey`) while a list/menu is open to flip just that one — the
main menu can stay LIST while a long submenu is switched to GRID. Each menu
remembers its choice for the session.

Lists start as **LIST** on every launch (the default is not persisted), so the
main menu is single-column unless you toggle it. `SettingsMenu → Display` sets the
default for the current session. Tables keep their own column layout.

The theme is a set of `MenuColor` slots (`MenuTheme.Slot`); hotkey rebindings are
stored by a hotkey's *default* key (`hotkey.F1=F3` moves the F1 action to F3).
Actions are code and are never serialized — only the key assignment is. Unknown
or malformed entries fall back to defaults, so a hand-edited file never breaks
loading.

## Try it (no host required)

A runnable demo (`com.xsoftwareforge.xconsoleui.demo.MenuDemo`) exercises navigation,
sub-menus, input/confirm/result screens and the buffered background logging
without any game server or plugin:

```
mvn -q compile exec:java
```

A background thread keeps emitting log lines; while a menu or wizard is on screen
they are buffered and replayed when you step back out. Use the arrow keys, Enter
to confirm, and Esc to go back (Esc at the root closes the menu). Run it in a real
terminal — not all IDE consoles support raw-mode cursor input.

## Build

```
mvn install
```

Installs `com.xsoftwareforge:xconsoleui:1.0.0` into the local `.m2` repository so
host projects can depend on it.

## License

XConsoleUI is released under the [MIT License](LICENSE). Copyright (c) 2026 Christopher Hahnen (XSoftwareForge).
