package org.uniprot.api.support.data.taxonomy.response;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class TaxonomyEntryConverter implements Function<TaxonomyDocument, TaxonomyEntry> {

    private final ObjectMapper objectMapper;

    public TaxonomyEntryConverter() {
        objectMapper = TaxonomyJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public TaxonomyEntry apply(TaxonomyDocument taxonomyDocument) {
        try {
            return objectMapper.readValue(
                    taxonomyDocument.getTaxonomyObj().array(), TaxonomyEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to TaxonomyEntry: ", e);
        }
        return null;
    }
}
