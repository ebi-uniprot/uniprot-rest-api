package org.uniprot.api.proteome.service;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.core.genecentric.GeneCentricEntry;
import org.uniprot.core.json.parser.genecentric.GeneCentricJsonConfig;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 17 May 2019
 */
public class GeneCentricEntryConverter implements Function<GeneCentricDocument, GeneCentricEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneCentricEntryConverter.class);

    private final ObjectMapper objectMapper;

    public GeneCentricEntryConverter() {
        objectMapper = GeneCentricJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public GeneCentricEntry apply(GeneCentricDocument t) {
        GeneCentricEntry entry = null;
        try {
            entry = objectMapper.readValue(t.getGeneCentricStored(), GeneCentricEntry.class);
            return entry;
        } catch (Exception e) {
            LOGGER.info("Error converting solr avro_binary default UniProtKBEntry", e);
        }
        return entry;
    }
}
