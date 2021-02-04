package org.uniprot.api.support.data.keyword.controller;

import java.nio.ByteBuffer;
import java.util.Collections;

import org.uniprot.core.cv.go.impl.GoTermBuilder;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.impl.StatisticsBuilder;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.search.document.keyword.KeywordDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author sahmad
 * @created 21/01/2021
 */
public class KeywordITUtils {
    public static KeywordDocument createSolrDocument(String keywordId, boolean facet) {
        KeywordId keyword =
                new KeywordIdBuilder().name("my keyword " + keywordId).id(keywordId).build();
        KeywordId category = new KeywordIdBuilder().name("Ligand").id("KW-9993").build();

        KeywordEntry keywordEntry =
                new KeywordEntryBuilder()
                        .definition("Definition value")
                        .keyword(keyword)
                        .category(category)
                        .synonymsAdd("synonyms")
                        .parentsAdd(new KeywordEntryBuilder().keyword(keyword).build())
                        .childrenAdd(new KeywordEntryBuilder().keyword(keyword).build())
                        .geneOntologiesAdd(
                                new GoTermBuilder().id("idValue").name("nameValue").build())
                        .sitesAdd("siteValue")
                        .statistics(new StatisticsBuilder().build())
                        .build();

        KeywordDocument document =
                KeywordDocument.builder()
                        .id(keywordId)
                        .name("my keyword " + keywordId)
                        .definition("Definition value " + keywordId)
                        .ancestor(Collections.singletonList("ancestor"))
                        .parent(Collections.singletonList("parent"))
                        .synonyms(Collections.singletonList("content"))
                        .keywordObj(getKeywordBinary(keywordEntry))
                        .build();
        return document;
    }

    private static ByteBuffer getKeywordBinary(KeywordEntry entry) {
        try {
            return ByteBuffer.wrap(
                    KeywordJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse KeywordEntry to binary json: ", e);
        }
    }
}
