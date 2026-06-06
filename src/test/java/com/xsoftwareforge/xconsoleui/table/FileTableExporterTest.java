package com.xsoftwareforge.xconsoleui.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileTableExporterTest {

    private Path dir;

    @Before
    public void setUp() throws IOException {
        dir = Files.createTempDirectory("console-menu-export");
    }

    @After
    public void tearDown() throws IOException {
        if (dir != null && Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // best effort
                    }
                });
            }
        }
    }

    @Test
    public void writesBothFormatsWithSanitizedName() throws IOException {
        Table table = TableBuilder.create().column("A").row("1").build();
        FileTableExporter exporter = new FileTableExporter(dir);

        List<Path> written = exporter.export(table, "../weird/name.txt", EnumSet.of(ExportFormat.CSV, ExportFormat.JSON));

        assertEquals(2, written.size());
        Path csv = dir.resolve("name.csv");
        Path json = dir.resolve("name.json");
        assertTrue(Files.exists(csv));
        assertTrue(Files.exists(json));
        assertEquals(TableSerializer.toCsv(table), Files.readString(csv));
        assertEquals(TableSerializer.toJson(table), Files.readString(json));
    }

    @Test
    public void traversalNameStaysInsideTargetDirectory() throws IOException {
        Table table = TableBuilder.create().column("A").row("1").build();
        FileTableExporter exporter = new FileTableExporter(dir);

        List<Path> written = exporter.export(table, "..\\..\\etc\\passwd", EnumSet.of(ExportFormat.CSV));

        assertEquals(1, written.size());
        Path out = written.get(0);
        // The export must never escape the target directory.
        assertEquals(dir.toRealPath(), out.toRealPath().getParent());
        assertEquals("passwd.csv", out.getFileName().toString());
    }
}
