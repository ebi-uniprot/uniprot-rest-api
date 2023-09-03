package org.uniprot.api.rest.download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/** Contains utility methods that aid in testing */
public final class TestUtils {

    public static void uncompressFile(Path zippedFile, Path unzippedFile) throws IOException {
        InputStream fin = Files.newInputStream(zippedFile);
        BufferedInputStream in = new BufferedInputStream(fin);
        OutputStream out = Files.newOutputStream(unzippedFile);
        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
        final byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = gzIn.read(buffer))) {
            out.write(buffer, 0, n);
        }
        out.close();
        gzIn.close();
    }
}
