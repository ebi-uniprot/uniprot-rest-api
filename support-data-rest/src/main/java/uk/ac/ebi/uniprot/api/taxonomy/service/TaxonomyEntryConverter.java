package uk.ac.ebi.uniprot.api.taxonomy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;

import java.util.function.Function;

@Slf4j
public class TaxonomyEntryConverter implements Function<TaxonomyDocument, TaxonomyEntry> {

    private final ObjectMapper objectMapper;

    public TaxonomyEntryConverter(){
        objectMapper = TaxonomyJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public TaxonomyEntry apply(TaxonomyDocument taxonomyDocument) {
        try {
            return objectMapper.readValue(taxonomyDocument.getTaxonomyObj().array(), TaxonomyEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to TaxonomyEntry: ", e);
        }
        return null;
    }

}
