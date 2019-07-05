package uk.ac.ebi.uniprot.api.literature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.json.parser.literature.LiteratureJsonConfig;
import uk.ac.ebi.uniprot.search.document.literature.LiteratureDocument;

import java.util.function.Function;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Slf4j
public class LiteratureEntryConverter implements Function<LiteratureDocument, LiteratureEntry> {

    private final ObjectMapper objectMapper;

    public LiteratureEntryConverter() {
        objectMapper = LiteratureJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public LiteratureEntry apply(LiteratureDocument literatureDocument) {
        try {
            return objectMapper.readValue(literatureDocument.getLiteratureObj().array(), LiteratureEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to LiteratureEntry: ", e);
        }
        return null;
    }

}
