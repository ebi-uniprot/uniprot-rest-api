package org.uniprot.api.uniprotkb.service;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author lgonzales
 * @since 2019-12-09
 */
@Slf4j
@Component
public class LiteratureStoreEntryConverter
        implements Function<LiteratureDocument, LiteratureStoreEntry> {

    private final ObjectMapper objectMapper;

    public LiteratureStoreEntryConverter() {
        objectMapper = LiteratureJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public LiteratureStoreEntry apply(LiteratureDocument literatureDocument) {
        try {
            return objectMapper.readValue(
                    literatureDocument.getLiteratureObj().array(), LiteratureStoreEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to LiteratureStoreEntry: ", e);
        }
        return null;
    }
}
