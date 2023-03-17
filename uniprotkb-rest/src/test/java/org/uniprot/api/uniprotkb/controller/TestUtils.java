package org.uniprot.api.uniprotkb.controller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.uniprot.core.flatfile.parser.SupportingDataMap;
import org.uniprot.core.flatfile.parser.impl.SupportingDataMapImpl;
import org.uniprot.core.flatfile.parser.impl.entry.EntryObjectConverter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/** Contains utility methods that aid in testing */
public final class TestUtils {
    private static final SupportingDataMap dataMap =
            new SupportingDataMapImpl("keywlist.txt", "humdisease.txt", null, null);
    private static final EntryObjectConverter entryObjectConverter =
            new EntryObjectConverter(dataMap, true);

    private TestUtils() {}

    static UniProtKBEntry convertToUniProtEntry(UniProtEntryObjectProxy objectProxy) {
        return objectProxy.convertToUniProtEntry(entryObjectConverter);
    }

    static InputStream getResourceAsStream(String resourcePath) {
        return TestUtils.class.getResourceAsStream(resourcePath);
    }

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
