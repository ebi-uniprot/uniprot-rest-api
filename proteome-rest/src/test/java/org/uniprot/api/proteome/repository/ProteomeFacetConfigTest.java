package org.uniprot.api.proteome.repository;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.proteome.controller.DataStoreTestConfig;
import org.uniprot.api.rest.download.AsyncDownloadMocks;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, AsyncDownloadMocks.class})
@ActiveProfiles(profiles = "offline")
@Import(ProteomeFacetConfig.class)
class ProteomeFacetConfigTest {
    @Autowired ProteomeFacetConfig config;

    @Test
    void testGetFacetPropertyMap() {

        Map<String, FacetProperty> map = config.getFacetPropertyMap();
        assertEquals(2, map.size());
    }

    @Test
    void testGetFacetNames() {

        Collection<String> map = config.getFacetNames();
        assertThat(map, hasItems("superkingdom", "proteome_type"));
    }
}
