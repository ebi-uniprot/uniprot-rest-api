package org.uniprot.api.support.data.literature.response;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Slf4j
@Component
public class LiteratureEntryConverter implements Function<LiteratureDocument, LiteratureEntry> {

    private final ObjectMapper objectMapper;

    public LiteratureEntryConverter() {
        objectMapper = LiteratureJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public LiteratureEntry apply(LiteratureDocument literatureDocument) {
        LiteratureEntry result = null;
        try {
            result =
                    objectMapper.readValue(
                            literatureDocument.getLiteratureObj().array(), LiteratureEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to LiteratureEntry: ", e);
        }
        return result;
    }
}
