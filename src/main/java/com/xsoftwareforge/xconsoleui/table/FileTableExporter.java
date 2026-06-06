package com.xsoftwareforge.xconsoleui.table;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@link TableExporter} that writes files into a fixed directory supplied by the
 * host. The base name is sanitized (path separators and extensions stripped) so
 * exported files always stay inside the target directory.
 */
public final class FileTableExporter implements TableExporter {

    private final Path directory;

    public FileTableExporter(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    @Override
    public List<Path> export(Table table, String baseName, Set<ExportFormat> formats) throws IOException {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(formats, "formats");
        String safeName = sanitize(baseName);
        Files.createDirectories(directory);

        List<Path> written = new ArrayList<>();
        for (ExportFormat format : formats) {
            Path target = directory.resolve(safeName + "." + format.extension());
            String content = switch (format) {
            case CSV -> TableSerializer.toCsv(table);
            case JSON -> TableSerializer.toJson(table);
            };
            Files.writeString(target, content, StandardCharsets.UTF_8);
            written.add(target);
        }
        return written;
    }

    private static String sanitize(String baseName) {
        String name = baseName == null ? "" : baseName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? "table" : name;
    }
}
