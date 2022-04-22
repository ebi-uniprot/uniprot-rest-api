package org.uniprot.api.common.repository.search.request;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.uniprot.api.common.repository.search.SolrQueryConfigCreationException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultTermExtractorTest {
    @ParameterizedTest
    @CsvSource({
        "a, a",
        "field:a,",
        "a b, a|b",
        "a b a, a|b",
        "cdc7, cdc7",
        "cdc7 AND (reviewed:true), cdc7",
        "HGNC\\:3689 AND (reviewed:true), HGNC\\:3689",
        "a (b:value OR (c:value AND d OR (e:value f) AND g)), a|d|f|g"
    })
    void extractsCorrectly(String query, String result) {
        String[] parts = result != null ? result.strip().split("\\|") : new String[0];
        Set<String> expectedDefaultTerms = new HashSet<>();
        if (parts.length > 0) {
            Collections.addAll(expectedDefaultTerms, parts);
        }

        Set<String> computedDefaultTerms = DefaultTermExtractor.extractDefaultTerms(query);

        MatcherAssert.assertThat(computedDefaultTerms, CoreMatchers.equalTo(expectedDefaultTerms));
    }
}
