package com.xsoftwareforge.xconsoleui.screen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for the pure escape-sequence key mapping in {@link KeyParser}.
 * These guard against regressions such as F5 ({@code ESC [ 1 5 ~}) being misread
 * as Home ({@code ESC [ 1 ~}).
 */
public class JLineKeyMappingTest {

    @Test
    public void mapsLetterTerminatedKeys() {
        assertEquals(Key.UP, KeyParser.mapLetter('A'));
        assertEquals(Key.DOWN, KeyParser.mapLetter('B'));
        assertEquals(Key.RIGHT, KeyParser.mapLetter('C'));
        assertEquals(Key.LEFT, KeyParser.mapLetter('D'));
        assertEquals(Key.HOME, KeyParser.mapLetter('H'));
        assertEquals(Key.END, KeyParser.mapLetter('F'));
    }

    @Test
    public void mapsSs3FunctionKeys() {
        assertEquals(Key.F1, KeyParser.mapLetter('P'));
        assertEquals(Key.F2, KeyParser.mapLetter('Q'));
        assertEquals(Key.F3, KeyParser.mapLetter('R'));
        assertEquals(Key.F4, KeyParser.mapLetter('S'));
    }

    @Test
    public void unknownLetterIsNull() {
        assertNull(KeyParser.mapLetter('Z'));
        assertNull(KeyParser.mapLetter('1'));
    }

    @Test
    public void mapsNavigationNumberKeys() {
        assertEquals(Key.HOME, KeyParser.mapNumberKey(1));
        assertEquals(Key.HOME, KeyParser.mapNumberKey(7));
        assertEquals(Key.END, KeyParser.mapNumberKey(4));
        assertEquals(Key.END, KeyParser.mapNumberKey(8));
        assertEquals(Key.PAGE_UP, KeyParser.mapNumberKey(5));
        assertEquals(Key.PAGE_DOWN, KeyParser.mapNumberKey(6));
    }

    @Test
    public void mapsFunctionNumberKeys() {
        assertEquals(Key.F5, KeyParser.mapNumberKey(15));
        assertEquals(Key.F6, KeyParser.mapNumberKey(17));
        assertEquals(Key.F10, KeyParser.mapNumberKey(21));
        assertEquals(Key.F12, KeyParser.mapNumberKey(24));
    }

    @Test
    public void f5IsNotConfusedWithHome() {
        assertEquals(Key.HOME, KeyParser.mapNumberKey(1));  // ESC [ 1 ~
        assertEquals(Key.F5, KeyParser.mapNumberKey(15));   // ESC [ 1 5 ~
    }

    @Test
    public void insertAndDeleteAreIgnored() {
        assertEquals(Key.UNKNOWN, KeyParser.mapNumberKey(2)); // Insert
        assertEquals(Key.UNKNOWN, KeyParser.mapNumberKey(3)); // Delete
    }
}
