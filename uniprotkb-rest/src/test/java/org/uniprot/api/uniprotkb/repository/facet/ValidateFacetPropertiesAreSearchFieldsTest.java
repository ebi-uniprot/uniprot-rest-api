package org.uniprot.api.uniprotkb.repository.facet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * Created 15/01/2020
 *
 * @author Edd
 */
@ActiveProfiles(profiles = "offline")
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
class ValidateFacetPropertiesAreSearchFieldsTest {
    @Autowired private UniprotKBFacetConfig config;
    private SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);

    @Test
    void validateAllFacetFieldsAreSearchFields() {
        config.getFacetNames()
                .forEach(
                        facetField -> {
                            assertThat(
                                    searchFieldConfig.searchFieldItemExists(facetField), is(true));
                        });
    }

    @TestConfiguration
    @Import(UniprotKBFacetConfig.class)
    static class TestFacetConfig {}
}
