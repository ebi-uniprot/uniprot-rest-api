package org.uniprot.api.uniprotkb.controller;

import java.io.InputStream;

import org.uniprot.core.flatfile.parser.SupportingDataMap;
import org.uniprot.core.flatfile.parser.impl.SupportingDataMapImpl;
import org.uniprot.core.flatfile.parser.impl.entry.EntryObjectConverter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/** Contains utility methods that aid in testing */
final class TestUtils {
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
}
