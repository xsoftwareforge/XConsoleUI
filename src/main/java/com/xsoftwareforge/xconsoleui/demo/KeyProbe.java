package com.xsoftwareforge.xconsoleui.demo;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Diagnostic helper that prints the raw byte codes produced by each key press in
 * raw mode. Useful for finding out how a particular terminal encodes the arrow
 * keys (e.g. {@code 27 91 65} for CSI {@code ESC [ A}, or {@code 27 79 65} for
 * SS3 {@code ESC O A}).
 * <p>
 * Run with:
 *
 * <pre>
 *   mvn -q compile exec:java -Dexec.mainClass=com.xsoftwareforge.xconsoleui.demo.KeyProbe
 * </pre>
 *
 * Press keys to see their codes; press {@code x} to quit.
 */
public final class KeyProbe {

    private KeyProbe() {
    }

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            terminal.writer().println("Key probe. Press keys to see their raw codes; press 'x' to quit.");
            terminal.writer().println("An arrow key normally shows 3 codes, e.g. 27 91 65 or 27 79 65.");
            terminal.writer().flush();

            Attributes previous = terminal.enterRawMode();
            NonBlockingReader reader = terminal.reader();
            try {
                while (true) {
                    int first = reader.read();
                    if (first == -1) {
                        break;
                    }
                    StringBuilder line = new StringBuilder("codes: ").append(first);
                    // Drain any bytes that arrived together (e.g. an escape sequence).
                    int next;
                    while ((next = reader.read(80L)) != NonBlockingReader.READ_EXPIRED && next != -1) {
                        line.append(' ').append(next);
                    }
                    terminal.writer().print(line + "\r\n");
                    terminal.writer().flush();
                    if (first == 'x' || first == 'X') {
                        break;
                    }
                }
            } finally {
                terminal.setAttributes(previous);
                terminal.writer().println("\r\nProbe finished.");
                terminal.writer().flush();
            }
        }
    }
}
