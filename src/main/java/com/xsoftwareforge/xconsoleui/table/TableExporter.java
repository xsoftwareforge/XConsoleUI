package com.xsoftwareforge.xconsoleui.table;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Writes a table to one or more formats. The host supplies the implementation
 * (and thus decides <em>where</em> files go); the library ships
 * {@link FileTableExporter} for the common "write into a directory" case.
 */
public interface TableExporter {

    /**
     * Export {@code table} under {@code baseName} (without extension) in each of
     * {@code formats}.
     *
     * @return the locations written, for reporting back to the user
     */
    List<Path> export(Table table, String baseName, Set<ExportFormat> formats) throws IOException;
}
