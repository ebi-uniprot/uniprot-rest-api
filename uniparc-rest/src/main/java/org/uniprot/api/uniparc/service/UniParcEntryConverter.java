package org.uniprot.api.uniparc.service;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.core.json.parser.uniparc.UniParcJsonConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
public class UniParcEntryConverter implements Function<UniParcDocument, UniParcEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniParcEntryConverter.class);

    private final ObjectMapper objectMapper;

    public UniParcEntryConverter() {
        objectMapper = UniParcJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public UniParcEntry apply(UniParcDocument t) {
        try {
            UniParcEntry entry =
                    objectMapper.readValue(t.getEntryStored().array(), UniParcEntry.class);
            return entry;
        } catch (Exception e) {
            LOGGER.info("Error converting solr avro_binary default UniParcEntry", e);
        }
        return null;
    }
}
