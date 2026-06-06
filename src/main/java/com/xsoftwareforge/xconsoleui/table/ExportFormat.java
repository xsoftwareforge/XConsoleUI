package com.xsoftwareforge.xconsoleui.table;

/** Supported table export formats. */
public enum ExportFormat {

    CSV("csv"),
    JSON("json");

    private final String extension;

    ExportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }
}
