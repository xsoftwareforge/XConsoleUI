package com.xsoftwareforge.xconsoleui.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.nio.file.Path;

import com.xsoftwareforge.xconsoleui.config.MenuConfig;
import com.xsoftwareforge.xconsoleui.config.MenuConfigStore;
import com.xsoftwareforge.xconsoleui.config.PropertiesMenuConfigStore;
import com.xsoftwareforge.xconsoleui.config.SettingsMenu;
import com.xsoftwareforge.xconsoleui.menu.AbstractMenu;
import com.xsoftwareforge.xconsoleui.menu.BuiltinHotkeyOptions;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;
import com.xsoftwareforge.xconsoleui.menu.Hotkey;
import com.xsoftwareforge.xconsoleui.menu.HotkeyOption;
import com.xsoftwareforge.xconsoleui.menu.Menu;
import com.xsoftwareforge.xconsoleui.menu.MenuContext;
import com.xsoftwareforge.xconsoleui.menu.MenuItem;
import com.xsoftwareforge.xconsoleui.menu.MenuRegistry;
import com.xsoftwareforge.xconsoleui.menu.OptionColor;
import com.xsoftwareforge.xconsoleui.form.Choice;
import com.xsoftwareforge.xconsoleui.form.ChoiceField;
import com.xsoftwareforge.xconsoleui.form.Form;
import com.xsoftwareforge.xconsoleui.form.FormBuilder;
import com.xsoftwareforge.xconsoleui.form.FormController;
import com.xsoftwareforge.xconsoleui.form.IntField;
import com.xsoftwareforge.xconsoleui.form.ItemEditor;
import com.xsoftwareforge.xconsoleui.form.ListField;
import com.xsoftwareforge.xconsoleui.form.TextField;
import com.xsoftwareforge.xconsoleui.form.ToggleField;
import com.xsoftwareforge.xconsoleui.list.RowBrowser;
import com.xsoftwareforge.xconsoleui.list.TableBrowser;
import com.xsoftwareforge.xconsoleui.list.TableColumnDef;
import com.xsoftwareforge.xconsoleui.output.ConsoleOutputCoordinator;
import com.xsoftwareforge.xconsoleui.runner.MenuSession;
import com.xsoftwareforge.xconsoleui.table.Align;
import com.xsoftwareforge.xconsoleui.table.FileTableExporter;
import com.xsoftwareforge.xconsoleui.table.Table;
import com.xsoftwareforge.xconsoleui.table.TableBuilder;
import com.xsoftwareforge.xconsoleui.table.TableExporter;

/**
 * Standalone, runnable demonstration of the console-menu library. It needs no
 * game server and no plugin host, so the menu, the cursor navigation and the
 * buffered output can be exercised directly from this project.
 * <p>
 * Run with:
 *
 * <pre>
 *   mvn -q compile exec:java -Dexec.mainClass=com.xsoftwareforge.xconsoleui.demo.MenuDemo
 * </pre>
 *
 * A background thread keeps emitting "log" lines through the
 * {@link ConsoleOutputCoordinator}. While a menu or wizard is on screen those
 * lines are buffered and replayed once you step back out — exactly the behaviour
 * a real host gets for its asynchronous logging.
 */
public final class MenuDemo {

    private MenuDemo() {
    }

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            ConsoleOutputCoordinator coordinator = new ConsoleOutputCoordinator();
            coordinator.attachPrinter(line -> {
                terminal.writer().println(line);
                terminal.writer().flush();
            });

            // Persist demo settings (theme + hotkey rebindings) next to the working dir.
            MenuConfigStore store = new PropertiesMenuConfigStore(Path.of("console-menu-demo.properties"));
            MenuConfig config = store.load();

            MenuRegistry registry = new MenuRegistry();
            registry.addMenu(new DemoToolsMenu());
            registry.addMenu(new DemoColorsMenu());
            registry.addMenu(new DemoSettingsMenu());
            registry.addMenu(new DemoLongListMenu());
            registry.addMenu(buildBuilderDemoMenu());
            registry.addMenu(new DemoTableMenu(new FileTableExporter(Path.of("console-menu-exports"))));
            registry.addMenu(new DemoFormMenu());
            registry.addMenu(new DemoSocketsMenu());
            registry.addMenu(SettingsMenu.create(registry, config, store));

            // Built-in options (Help on F1, Close menu unbound) + a custom one.
            // Reassign any of them under Settings -> Hotkeys; choices persist.
            registry.addHotkeyOptions(BuiltinHotkeyOptions.all(registry));
            // F8 is the built-in per-list LIST/GRID toggle (handled by the screen).
            registry.addHotkeyOption(HotkeyOption.of("demo.ping", "Ping", FunctionKey.F5,
                    ctx -> ctx.printSuccess("pong")));

            AtomicBoolean noisy = new AtomicBoolean(true);
            Thread backgroundLogger = startBackgroundLogger(coordinator, noisy);

            coordinator.publish("Demo started. A background logger is running.");
            coordinator.publish("Open the menu, navigate around, and watch how these");
            coordinator.publish("background lines are held back while a screen is visible.");

            MenuSession session = new MenuSession(terminal, registry, coordinator, "console-menu demo", config);
            session.open();

            noisy.set(false);
            backgroundLogger.interrupt();
            coordinator.publish("Menu closed. Goodbye.");
            coordinator.detachPrinter();
        }
    }

    /** Demonstrates a form built from input components (Phase 3a). */
    private static final class DemoFormMenu extends AbstractMenu {

        private DemoFormMenu() {
            super("form", "Form (create rule demo)");
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(action("create", "Create a rule", ctx -> {
                FormBuilder fb = Form.builder("Create rule");
                TextField id = fb.text("id", "ID").required();
                ChoiceField scope = fb.choice("scope", "Scope",
                        List.of(Choice.of("storage", "Storage"), Choice.of("expansion", "Expansion"))).required();
                IntField priority = fb.intField("priority", "Priority").range(0, 1000);
                ToggleField enabled = fb.toggle("enabled", "Enabled");
                enabled.setValue(true);
                // A ListField whose item editor is itself a nested form (the "When" pattern).
                ListField<String> conditions = fb.list("when", "When (conditions)", c -> c, conditionEditor())
                        .required();

                if (FormController.open(ctx, fb.build())) {
                    ctx.printSuccess("Rule '" + id.value() + "' [" + scope.selectedId() + "] priority "
                            + priority.value() + " enabled=" + enabled.value());
                    ctx.print("  when: " + String.join(" && ", conditions.values()));
                } else {
                    ctx.print("Cancelled.");
                }
            }));
        }
    }

    /** Selectable list with type-ahead + per-row hotkey actions (like ListSockets). */
    private static final class DemoSocketsMenu extends AbstractMenu {

        private final List<String> sockets = new java.util.ArrayList<>(List.of(
                "alice@10.0.0.1", "bob@10.0.0.2", "carol@10.0.0.3", "dave@10.0.0.4",
                "erin@10.0.0.5", "frank@10.0.0.6", "grace@10.0.0.7", "heidi@10.0.0.8"));

        private DemoSocketsMenu() {
            super("sockets", "Sockets (selectable list)");
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(action("list", "Browse connected sockets", ctx ->
                    RowBrowser.create("Connected sockets", () -> sockets, s -> s)
                            .filterable(true)
                            .action(FunctionKey.F2, "Kick", (c, socket) -> {
                                sockets.remove(socket);
                                c.printSuccess("Kicked " + socket);
                            })
                            .action(FunctionKey.F3, "Info", (c, socket) ->
                                    c.showResult("Socket info", List.of("Socket: " + socket, "State: connected")))
                            .onSelect((c, socket) -> c.printSuccess("Selected " + socket))
                            .open(ctx)));
        }
    }

    /** Item editor for the "When" list: a nested form producing "path op value". */
    private static ItemEditor<String> conditionEditor() {
        return (ctx, current) -> {
            FormBuilder cf = Form.builder(current == null ? "Add condition" : "Edit condition");
            TextField path = cf.text("path", "Path").required();
            ChoiceField op = cf.choice("op", "Operator",
                    List.of(Choice.of(">=", ">="), Choice.of("==", "=="), Choice.of("<=", "<="))).required();
            TextField value = cf.text("value", "Value").required();
            if (current != null) {
                String[] parts = current.split(" ", 3);
                if (parts.length == 3) {
                    path.setValue(parts[0]);
                    op.select(parts[1]);
                    value.setValue(parts[2]);
                }
            }
            if (FormController.open(ctx, cf.build())) {
                return path.value() + " " + op.selectedId() + " " + value.value();
            }
            return null;
        };
    }

    /** Demonstrates the table builder with a fixed header and export. */
    private static final class DemoTableMenu extends AbstractMenu {

        private final TableExporter exporter;

        private DemoTableMenu(TableExporter exporter) {
            super("table", "Table (planets demo)");
            this.exporter = exporter;
        }

        private final List<Planet> planets = new java.util.ArrayList<>();

        {
            String[] names = {"Terra", "Mars", "Io", "Europa", "Titan", "Kepler-22b", "Proxima b", "Gliese"};
            for (int i = 0; i < 45; i++) {
                planets.add(new Planet(names[i % names.length] + "-" + (i + 1), "G" + (i % 9),
                        (i * 1234) % 100000, i % 4 == 0 ? "—" : "Player" + (i % 7)));
            }
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(
                    action("show", "Show planet table (export with 'e')",
                            ctx -> ctx.showTable("Planets", buildPlanetTable(), exporter)),
                    action("browse", "Browse planets (Tab=cell mode, F2 action, type to filter)", ctx ->
                            TableBrowser.create("Planets", () -> planets)
                                    .column(TableColumnDef.of("Planet", Planet::name))
                                    .column(TableColumnDef.of("Galaxy", Planet::galaxy))
                                    .column(TableColumnDef.of("Pop", Align.RIGHT, p -> String.valueOf(p.pop())))
                                    .column(TableColumnDef.of("Owner", Planet::owner).editable((c, p) -> {
                                        String value = c.input("Owner of " + p.name(), p.owner());
                                        if (value != null) {
                                            p.setOwner(value);
                                            c.printSuccess("Owner set to " + value);
                                        }
                                    }))
                                    .action(FunctionKey.F2, "Abandon", (c, p) -> {
                                        planets.remove(p);
                                        c.printSuccess("Abandoned " + p.name());
                                    })
                                    .onSelect((c, p) -> c.showResult("Planet " + p.name(),
                                            List.of("Galaxy: " + p.galaxy(), "Population: " + p.pop(),
                                                    "Owner: " + p.owner())))
                                    .open(ctx)));
        }

        private static final class Planet {
            private final String name;
            private final String galaxy;
            private final int pop;
            private String owner;

            Planet(String name, String galaxy, int pop, String owner) {
                this.name = name;
                this.galaxy = galaxy;
                this.pop = pop;
                this.owner = owner;
            }

            String name() {
                return name;
            }

            String galaxy() {
                return galaxy;
            }

            int pop() {
                return pop;
            }

            String owner() {
                return owner;
            }

            void setOwner(String owner) {
                this.owner = owner;
            }
        }

        private Table buildPlanetTable() {
            TableBuilder builder = TableBuilder.create()
                    .column("Planet")
                    .column("Galaxy")
                    .column("Pop", Align.RIGHT)
                    .column("Owner");
            for (Planet p : planets) {
                builder.row(p.name(), p.galaxy(), p.pop(), p.owner());
            }
            return builder.build();
        }
    }

    /** A long menu to demonstrate paging (PgUp/PgDn/Home/End) and scrolling. */
    private static final class DemoLongListMenu extends AbstractMenu {

        private DemoLongListMenu() {
            super("long", "Long list (paging demo)");
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            List<MenuItem> items = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                int n = i;
                items.add(action("entry-" + n, String.format("Entry %02d", n),
                        ctx -> ctx.printSuccess("You picked entry " + n + ".")));
            }
            return items;
        }
    }

    /** Shows the fluent {@link Menu#builder} API instead of subclassing AbstractMenu. */
    private static Menu buildBuilderDemoMenu() {
        return Menu.builder("builder", "Built with MenuBuilder")
                .action("hello", "Say hello", ctx -> ctx.printSuccess("Hello from a built menu!"))
                .action("warn", "A red entry", OptionColor.RED, ctx -> ctx.print("Careful."))
                .submenu("nested", "Nested (inline builder)", nested -> nested
                        .action("deep", "A deeply nested action", ctx -> ctx.print("You went deep."))
                        .action("empty", "Open an empty sub-menu",
                                ctx -> ctx.select("This menu is empty", java.util.List.of())))
                .build();
    }

    private static Thread startBackgroundLogger(ConsoleOutputCoordinator coordinator, AtomicBoolean noisy) {
        AtomicInteger counter = new AtomicInteger();
        Thread thread = new Thread(() -> {
            while (noisy.get()) {
                try {
                    Thread.sleep(1500L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                coordinator.publish("[background] tick #" + counter.incrementAndGet());
            }
        }, "demo-background-logger");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Top-level menu with leaf actions that use the interactive primitives. */
    private static final class DemoToolsMenu extends AbstractMenu {

        private DemoToolsMenu() {
            super("tools", "Tools (input / confirm / result)");
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(
                    action("greet", "Ask for a name and greet", ctx -> {
                        String name = ctx.input("What is your name?", "stranger");
                        if (name == null) {
                            ctx.print("Cancelled.");
                            return;
                        }
                        ctx.printSuccess("Hello, " + name + "!");
                    }),
                    action("secret", "Enter a masked secret", ctx -> {
                        String secret = ctx.inputMasked("Type a secret");
                        if (secret == null) {
                            ctx.print("Cancelled.");
                            return;
                        }
                        ctx.printSuccess("Stored a secret of length " + secret.length() + ".");
                    }),
                    action("danger", "Confirm a dangerous action", OptionColor.RED, ctx -> {
                        boolean ok = ctx.confirm("Really do the dangerous thing?", false);
                        ctx.print(ok ? "Boom!" : "Phew, nothing happened.");
                    }),
                    action("report", "Show a scrollable report", ctx -> {
                        List<String> lines = new ArrayList<>();
                        for (int i = 1; i <= 60; i++) {
                            lines.add(String.format("Report line %02d - lorem ipsum dolor sit amet", i));
                        }
                        ctx.showResult("Generated report (60 lines)", lines);
                    }),
                    action("grid", "Show 120 short items (toggle LIST/GRID with F8)", ctx -> {
                        List<String> items = new ArrayList<>();
                        for (int i = 1; i <= 120; i++) {
                            items.add(String.format("item-%03d", i));
                        }
                        ctx.showResult("Short items", items);
                    }),
                    action("flood", "Print 5 lines (watch them flush)", ctx -> {
                        for (int i = 1; i <= 5; i++) {
                            ctx.print("foreground line " + i);
                        }
                    }));
        }
    }

    /** Demonstrates the colored option styles. */
    private static final class DemoColorsMenu extends AbstractMenu {

        private DemoColorsMenu() {
            super("colors", "Colored entries");
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(
                    action("default", "Default colored entry", OptionColor.DEFAULT,
                            ctx -> ctx.print("You picked the default entry.")),
                    action("green", "Green entry", OptionColor.GREEN,
                            ctx -> ctx.print("You picked the green entry.")),
                    action("white", "White entry", OptionColor.WHITE,
                            ctx -> ctx.print("You picked the white entry.")),
                    action("red", "Red / destructive entry", OptionColor.RED,
                            ctx -> ctx.print("You picked the red entry.")));
        }
    }

    /** Demonstrates nested sub-menus and dynamic, state-dependent items. */
    private static final class DemoSettingsMenu extends AbstractMenu {

        private boolean verbose;
        private boolean autosave = true;

        private DemoSettingsMenu() {
            // Distinct id so it does not collide with the real SettingsMenu (id "settings").
            super("demo-settings", "Settings (nested + dynamic)");
        }

        @Override
        public List<MenuItem> items(MenuContext context) {
            return List.of(
                    action("verbose", "Verbose logging: " + onOff(verbose), ctx -> verbose = !verbose),
                    action("autosave", "Autosave: " + onOff(autosave), ctx -> autosave = !autosave),
                    submenu("advanced", "Advanced...", new AdvancedSettingsMenu()));
        }

        @Override
        public List<Hotkey> hotkeys() {
            // F1 overrides the global help; F2 is a settings-only hotkey.
            return List.of(
                    new Hotkey(FunctionKey.F1, "Settings help", ctx -> ctx.showResult("Settings help (F1)",
                            java.util.List.of("This F1 is specific to the Settings menu.",
                                    "It overrides the global F1 help while you are here."))),
                    new Hotkey(FunctionKey.F2, "Reset", ctx -> {
                        verbose = false;
                        autosave = true;
                        ctx.printSuccess("Settings reset to defaults.");
                    }));
        }

        private static String onOff(boolean value) {
            return value ? "[on]" : "[off]";
        }

        private static final class AdvancedSettingsMenu extends AbstractMenu {

            private AdvancedSettingsMenu() {
                super("advanced", "Advanced settings");
            }

            @Override
            public List<MenuItem> items(MenuContext context) {
                return List.of(
                        action("reset", "Reset everything", OptionColor.RED, ctx -> {
                            if (ctx.confirm("Reset all advanced settings?", false)) {
                                ctx.printSuccess("Advanced settings reset.");
                            } else {
                                ctx.print("Reset aborted.");
                            }
                        }),
                        action("about", "About this sub-menu", ctx -> ctx.showResult("About",
                                List.of("This is a nested sub-menu.",
                                        "Press Esc to go back one level.",
                                        "Esc at the root closes the menu."))));
            }
        }
    }
}
