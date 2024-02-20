package org.uniprot.api.uniprotkb;

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
import org.uniprot.api.rest.download.AsyncDownloadMocks;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
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
@SpringBootTest(
        classes = {
            UniProtKBDataStoreTestConfig.class,
            AsyncDownloadMocks.class,
            UniProtKBREST.class
        })
class ValidateFacetPropertiesAreSearchFieldsTest {
    @Autowired private UniProtKBFacetConfig config;
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
    @Import(UniProtKBFacetConfig.class)
    static class TestFacetConfig {}
}
