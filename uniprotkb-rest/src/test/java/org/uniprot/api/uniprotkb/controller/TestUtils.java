package org.uniprot.api.uniprotkb.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.uniprot.core.flatfile.parser.SupportingDataMap;
import org.uniprot.core.flatfile.parser.impl.SupportingDataMapImpl;
import org.uniprot.core.flatfile.parser.impl.entry.EntryObjectConverter;
import org.uniprot.core.uniprot.UniProtEntry;

/**
 * Contains utility methods that aid in testing
 */
final class TestUtils {
	private static final SupportingDataMap dataMap = new SupportingDataMapImpl("keywlist.txt",
			"humdisease.txt", null, null	
			);
    private static final EntryObjectConverter entryObjectConverter = new EntryObjectConverter(dataMap, true);

    private TestUtils() {
    }

    static UniProtEntry convertToUniProtEntry(UniProtEntryObjectProxy objectProxy) {
        return objectProxy.convertToUniProtEntry(entryObjectConverter);
    }

    InputStream getResourceAsStream(String resourcePath) {
        return TestUtils.class.getResourceAsStream(resourcePath);
    }

    public static String convertInputStreamToString(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (InputStreamReader isr = new InputStreamReader(stream);
             BufferedReader br = new BufferedReader(isr)) {
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a flatfile like line which contains multiple elements within it.
     * <br/>
     * Examples of such lines:
     * <ul>
     * <li>Keywords KW</li>
     * <li>Organism classification (OC)</li>
     * </ul>
     *
     * @param lineStart      the starting string of the line
     * @param separator      the string character that separates the elements
     * @param lineTerminator indicates that all of the elements have been inserted
     * @param elements       the elements to populate the line
     * @return the populated line
     */
    public static String createMultiElementFFLine(String lineStart, String separator, String lineTerminator,
                                                  String... elements) {
        StringBuilder line = new StringBuilder(lineStart);

        if (elements.length > 0) {
            for (String element : elements) {
                line.append(element).append(separator).append(" ");
            }

            line.replace(line.length() - 2, line.length(), lineTerminator);
        } else {
            line.append(".");
        }

        return line.toString();
    }
}