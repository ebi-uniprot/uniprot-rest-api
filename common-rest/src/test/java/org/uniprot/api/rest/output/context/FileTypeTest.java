package org.uniprot.api.rest.output.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 * @since 28/07/2020
 */
class FileTypeTest {

    @Test
    void getExtension() {
        assertEquals(".gz", FileType.GZIP.getExtension());
    }

    @Test
    void getFileType() {
        assertEquals("file", FileType.FILE.getFileType());
    }

    @Test
    void bestFileTypeMatchGzip() {
        assertEquals(FileType.GZIP, FileType.bestFileTypeMatch("gzip"));
    }

    @Test
    void bestFileTypeMatchFile() {
        assertEquals(FileType.FILE, FileType.bestFileTypeMatch("file"));
    }

    @Test
    void bestFileTypeMatchInvalidDefaultToFile() {
        assertEquals(FileType.FILE, FileType.bestFileTypeMatch("invalid"));
    }
}
