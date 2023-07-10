package org.uniprot.api.support.data.common.keyword.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.search.document.keyword.KeywordDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

class KeywordEntryConverterTest {

    @Test
    void canConvertKeywordWithSuccess() throws JsonProcessingException {
        KeywordEntry entry = getKeywordEntry();
        byte[] entryInBytes =
                KeywordJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry);
        KeywordDocument doc = getKeywordDocument(entryInBytes);

        KeywordEntryConverter keywordEntryConverter = new KeywordEntryConverter();
        KeywordEntry result = keywordEntryConverter.apply(doc);
        assertNotNull(result);
        assertEquals(entry, result);
    }

    @Test
    void canNotConvertKeywordWithSimpleMapper() throws JsonProcessingException {
        KeywordEntry entry = getKeywordEntry();
        byte[] entryInBytes =
                KeywordJsonConfig.getInstance().getSimpleObjectMapper().writeValueAsBytes(entry);
        KeywordDocument doc = getKeywordDocument(entryInBytes);

        KeywordEntryConverter keywordEntryConverter = new KeywordEntryConverter();
        KeywordEntry result = keywordEntryConverter.apply(doc);
        assertNull(result);
    }

    private KeywordEntry getKeywordEntry() {
        return new KeywordEntryBuilder()
                .category(new KeywordIdBuilder().id("KW-0001").name("Name").build())
                .definition("KW Definition")
                .linksAdd("KW link")
                .build();
    }

    private KeywordDocument getKeywordDocument(byte[] entryInBytes) {
        return KeywordDocument.builder()
                .id("KW-0001")
                .keywordObj(ByteBuffer.wrap(entryInBytes))
                .build();
    }
}
