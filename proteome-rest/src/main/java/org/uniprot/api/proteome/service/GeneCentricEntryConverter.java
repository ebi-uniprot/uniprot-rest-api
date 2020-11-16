package org.uniprot.api.proteome.service;

import java.util.function.Function;

import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.core.json.parser.genecentric.GeneCentricJsonConfig;
import org.uniprot.store.search.document.genecentric.GeneCentricDocument;
import org.uniprot.store.search.document.genecentric.GeneCentricDocumentConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 17 May 2019
 */
public class GeneCentricEntryConverter implements Function<GeneCentricDocument, GeneCentricEntry> {

    private final GeneCentricDocumentConverter converter;

    public GeneCentricEntryConverter() {
        ObjectMapper objectMapper = GeneCentricJsonConfig.getInstance().getFullObjectMapper();
        converter = new GeneCentricDocumentConverter(objectMapper);
    }

    @Override
    public GeneCentricEntry apply(GeneCentricDocument document) {
        return converter.getCanonicalEntryFromDocument(document);
    }
}
