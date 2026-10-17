package com.xsoftwareforge.xconsoleui.screen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.terminal.Terminal;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.Test;

import com.xsoftwareforge.xconsoleui.menu.BrowseResult;
import com.xsoftwareforge.xconsoleui.menu.BrowseState;
import com.xsoftwareforge.xconsoleui.menu.FunctionKey;

/**
 * Drives the real {@link JLineTerminalScreen} row browser with scripted key bytes
 * to cover the multi-select key handling (Space, Ctrl+A, Esc).
 */
public class MultiSelectBrowseTest {

    private static final int PAUSE = -2;
    private static final int ESC = 27;
    private static final int CTRL_A = 1;
    private static final int ENTER = '\r';
    private static final int BACKSPACE = 127;
    private static final int[] DOWN = {ESC, '[', 'B'};

    private ByteArrayOutputStream out;

    @Test
    public void spaceTogglesTheCursorRow() throws Exception {
        BrowseState state = new BrowseState();
        BrowseResult result = browse(List.of("a", "b", "c"), state,
                keys(DOWN, ' ', DOWN, ' ', ' ', ENTER));

        assertEquals(BrowseResult.Kind.SELECT, result.kind());
        assertEquals(2, result.index());
        assertEquals(Set.of(1), state.marked());
        assertEquals(2, state.row());
        String screen = out.toString(StandardCharsets.UTF_8);
        assertTrue(screen.contains("[x] b"));
        assertTrue(screen.contains("[ ] a"));
        assertTrue(screen.contains("1 markiert"));
        assertTrue(screen.contains("Space mark, Ctrl+A mark all"));
    }

    @Test
    public void ctrlAMarksVisibleRowsUnderTheFilterAndMarksSurviveFilterChanges() throws Exception {
        BrowseState state = new BrowseState();
        List<String> rows = List.of("alpha", "beta", "alpine", "gamma");

        // "al" -> alpha, alpine visible; Ctrl+A marks them; clear filter; "gam" + Space marks gamma.
        browse(rows, state, keys('a', 'l', CTRL_A, BACKSPACE, BACKSPACE, 'g', 'a', 'm', ' ', ENTER));
        assertEquals(Set.of(0, 2, 3), state.marked());
        assertEquals("filter is carried in the state", "gam", state.filter());

        // Re-open: filter "gam" restored, Ctrl+A on a fully marked view unmarks only gamma.
        browse(rows, state, keys(CTRL_A, ENTER));
        assertEquals(Set.of(0, 2), state.marked());
    }

    @Test
    public void escapeClearsMarksFirstThenLeaves() throws Exception {
        BrowseState state = new BrowseState();
        BrowseResult result = browse(List.of("a", "b"), state, keys(' ', DOWN, ' ', ESC, PAUSE, ESC, PAUSE));

        assertTrue(result.isBack());
        assertEquals(Set.of(), state.marked());
    }

    @Test
    public void functionKeyReturnsActionWithMarksKept() throws Exception {
        BrowseState state = new BrowseState();
        BrowseResult result = browse(List.of("a", "b", "c"), state,
                keys(' ', DOWN, DOWN, ' ', ESC, '[', '1', '5', '~'), Map.of(FunctionKey.F5, "Run"));

        assertEquals(BrowseResult.Kind.ACTION, result.kind());
        assertEquals(FunctionKey.F5, result.actionKey());
        assertEquals(2, result.index());
        assertEquals(Set.of(0, 2), state.marked());
    }

    @Test
    public void plainBrowserStillTypesSpaceIntoTheFilter() throws Exception {
        Terminal terminal = terminal(keys('a', ' ', 'b', ENTER));
        try {
            BrowseResult result = new JLineTerminalScreen(terminal)
                    .browse("Plain", null, List.of("ab", "a b"), Map.of(), true);
            assertEquals(1, result.index());
            assertTrue(!out.toString(StandardCharsets.UTF_8).contains("markiert"));
        } finally {
            terminal.close();
        }
    }

    private BrowseResult browse(List<String> rows, BrowseState state, Deque<Integer> input) throws Exception {
        return browse(rows, state, input, Map.of());
    }

    private BrowseResult browse(List<String> rows, BrowseState state, Deque<Integer> input,
            Map<FunctionKey, String> actions) throws Exception {
        Terminal terminal = terminal(input);
        try {
            return new JLineTerminalScreen(terminal).browse("Rows", null, rows, actions, true, state);
        } finally {
            terminal.close();
        }
    }

    /**
     * A terminal fed from a daemon thread; {@link #PAUSE} waits longer than the key
     * parser's Esc timeout so a lone Esc is recognised.
     */
    private Terminal terminal(Deque<Integer> input) throws IOException {
        out = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "xterm", out, StandardCharsets.UTF_8);
        terminal.setSize(new Size(100, 30));
        Thread feeder = new Thread(() -> {
            try {
                for (int b : input) {
                    if (b == PAUSE) {
                        Thread.sleep(300);
                    } else {
                        terminal.processInputByte(b);
                    }
                }
            } catch (IOException | InterruptedException ignored) {
                // terminal closed
            }
        });
        feeder.setDaemon(true);
        feeder.start();
        return terminal;
    }

    private static Deque<Integer> keys(Object... keys) {
        Deque<Integer> bytes = new ArrayDeque<>();
        for (Object key : keys) {
            if (key instanceof int[] sequence) {
                for (int b : sequence) {
                    bytes.add(b);
                }
            } else if (key instanceof Character c) {
                bytes.add((int) c);
            } else {
                bytes.add((Integer) key);
            }
        }
        return bytes;
    }
}
