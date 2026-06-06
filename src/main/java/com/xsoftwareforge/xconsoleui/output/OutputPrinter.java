package com.xsoftwareforge.xconsoleui.output;

/**
 * Sink for asynchronous console output that the coordinator pushes "above" the
 * current input line. Hosts typically back this by JLine's
 * {@code LineReader.printAbove(String)}; in standalone use it can simply write
 * to {@code System.out}.
 */
@FunctionalInterface
public interface OutputPrinter {

    void printAbove(String message);
}
