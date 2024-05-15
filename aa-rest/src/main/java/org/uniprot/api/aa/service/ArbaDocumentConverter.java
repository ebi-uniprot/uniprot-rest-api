package org.uniprot.api.aa.service;

import java.io.IOException;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.uniprot.core.json.parser.unirule.UniRuleJsonConfig;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.search.document.arba.ArbaDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Component
@Slf4j
public class ArbaDocumentConverter implements Function<ArbaDocument, UniRuleEntry> {
    private final ObjectMapper objectMapper;

    public ArbaDocumentConverter() {
        this.objectMapper = UniRuleJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public UniRuleEntry apply(ArbaDocument arbaDocument) {
        UniRuleEntry entry = null;
        try {
            entry =
                    this.objectMapper.readValue(
                            arbaDocument.getRuleObj().array(), UniRuleEntry.class);
        } catch (IOException e) {
            log.info("Error converting ARBA solr binary to UniRuleEntry: ", e);
        }
        return entry;
    }
}
