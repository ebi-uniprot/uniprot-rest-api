package org.uniprot.api.proteome.service;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */
public class ProteomeEntryConverter implements Function<ProteomeDocument, ProteomeEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProteomeEntryConverter.class);

    private final ObjectMapper objectMapper;

    public ProteomeEntryConverter() {
        objectMapper = ProteomeJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public ProteomeEntry apply(ProteomeDocument t) {
        try {
            ProteomeEntry entry =
                    objectMapper.readValue(
                            t.proteomeStored.array(),
                            org.uniprot.core.proteome.ProteomeEntry.class);
            return entry;
        } catch (Exception e) {
            LOGGER.info("Error converting solr avro_binary default UniProtKBEntry", e);
        }
        return null;
    }
}
