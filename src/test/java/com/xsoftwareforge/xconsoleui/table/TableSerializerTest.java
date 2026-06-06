package com.xsoftwareforge.xconsoleui.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableSerializerTest {

    private static Table sample() {
        return TableBuilder.create()
                .column("Name")
                .column("Note")
                .row("Earth", "home, sweet \"home\"")
                .row("Mars", "red")
                .build();
    }

    @Test
    public void csvQuotesFieldsWithCommasAndQuotes() {
        String csv = TableSerializer.toCsv(sample());
        String[] lines = csv.split("\n");
        assertEquals("Name,Note", lines[0]);
        // Comma + embedded quotes -> field quoted, inner quotes doubled.
        assertEquals("Earth,\"home, sweet \"\"home\"\"\"", lines[1]);
        assertEquals("Mars,red", lines[2]);
    }

    @Test
    public void csvQuotesFieldsWithEmbeddedNewlines() {
        Table table = TableBuilder.create()
                .column("Name")
                .column("Note")
                .row("Earth", "line one\nline two")
                .build();
        String csv = TableSerializer.toCsv(table);
        // A field containing a newline must be wrapped in quotes (RFC 4180),
        // so the record itself spans two physical lines.
        assertTrue(csv.contains("Earth,\"line one\nline two\""));
    }

    @Test
    public void jsonProducesArrayOfObjects() {
        String json = TableSerializer.toJson(sample());
        assertTrue(json.startsWith("["));
        assertTrue(json.trim().endsWith("]"));
        assertTrue(json.contains("\"Name\": \"Earth\""));
        assertTrue(json.contains("\"Note\": \"red\""));
        // Quotes inside a value are escaped.
        assertTrue(json.contains("\\\"home\\\""));
    }
}
