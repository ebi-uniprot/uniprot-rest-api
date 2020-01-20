package org.uniprot.api.uniprotkb.repository.facet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import org.uniprot.store.search.domain2.UniProtSearchFields;

/**
 * Created 15/01/2020
 *
 * @author Edd
 */
@SpringBootTest
class ValidateFacetPropertiesAreSearchFieldsTest {
    @Autowired private UniprotFacetConfig config;

    @Test
    void validateAllFacetFieldsAreSearchFields() {
        config.getFacetNames()
                .forEach(
                        facetField -> {
                            assertThat(
                                    UniProtSearchFields.UNIPROTKB.hasField(facetField), is(true));
                        });
    }

    @TestConfiguration
    @Import(UniprotFacetConfig.class)
    static class TestFacetConfig {}
}
