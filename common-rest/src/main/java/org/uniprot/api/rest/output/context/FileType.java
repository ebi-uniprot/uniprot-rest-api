package org.uniprot.api.rest.output.context;

/**
 * Created 10/09/18
 *
 * @author Edd
 */
public enum FileType {
    FILE("file", ""),
    GZIP("gzip", ".gz");

    private final String fileType;
    private final String extension;

    FileType(String fileType, String extension) {
        this.fileType = fileType;
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public String getFileType() {
        return fileType;
    }

    /**
     * @param value the requested file type
     * @return the most appropriate file type for the given input
     */
    public static FileType bestFileTypeMatch(String value) {
        for (FileType type : FileType.values()) {
            if (type.fileType.equals(value)) {
                return type;
            }
        }
        return FILE;
    }
}
