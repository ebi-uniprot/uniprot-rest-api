package org.uniprot.api.proteome.repository;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.facet.FacetProperty;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProteomeFacetConfigTest.TestConfig.class)
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

    @EnableConfigurationProperties(ProteomeFacetConfig.class)
    static class TestConfig {}
}
