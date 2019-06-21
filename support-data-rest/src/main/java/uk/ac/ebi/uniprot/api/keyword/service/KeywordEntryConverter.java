package uk.ac.ebi.uniprot.api.keyword.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.json.parser.keyword.KeywordJsonConfig;
import uk.ac.ebi.uniprot.search.document.keyword.KeywordDocument;

import java.util.function.Function;

/**
 * @author lgonzales
 */
@Slf4j
public class KeywordEntryConverter implements Function<KeywordDocument, KeywordEntry> {

    private final ObjectMapper objectMapper;

    public KeywordEntryConverter() {
        objectMapper = KeywordJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public KeywordEntry apply(KeywordDocument keywordDocument) {
        try {
            return objectMapper.readValue(keywordDocument.getKeywordObj().array(), KeywordEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to KeywordEntry: ", e);
        }
        return null;
    }

}
