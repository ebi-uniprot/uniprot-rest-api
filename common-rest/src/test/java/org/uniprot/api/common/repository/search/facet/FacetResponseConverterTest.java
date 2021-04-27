package org.uniprot.api.common.repository.search.facet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.IntervalFacet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;

/** @author lgonzales */
class FacetResponseConverterTest {

    private final QueryResponse queryResponse = mock(QueryResponse.class);

    @Test
    void convertFacetForNullResponse() {
        when(queryResponse.getFacetFields()).thenReturn(null);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse);
        assertNotNull(facets);
        assertEquals(0, facets.size());
    }

    @Test
    void convertFacetWithLabel() {
        List<FacetField> fieldList = getFacetFields("fragment", "reviewed");

        when(queryResponse.getFacetFields()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse);
        assertNotNull(facets);
        assertEquals(2, facets.size());

        Facet reviewed = facets.get(0);
        assertEquals("Status", reviewed.getLabel());
        assertEquals("reviewed", reviewed.getName());
        assertFalse(reviewed.isAllowMultipleSelection());
        assertNotNull(reviewed.getValues());
        assertEquals(2, reviewed.getValues().size());

        FacetItem itemValue = reviewed.getValues().get(0);
        assertNotNull(itemValue);
        assertEquals("Reviewed (Swiss-Prot)", itemValue.getLabel());
        assertEquals("true", itemValue.getValue());
        assertEquals(Long.valueOf(10L), itemValue.getCount());
    }

    @Test
    void convertFacetWithoutLabel() {
        List<FacetField> fieldList = getFacetFields("model_organism");

        when(queryResponse.getFacetFields()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse);
        assertNotNull(facets);

        Facet modelOrganism = facets.get(0);
        assertEquals("Model organisms", modelOrganism.getLabel());
        assertEquals("model_organism", modelOrganism.getName());
        assertTrue(modelOrganism.isAllowMultipleSelection());
        assertNotNull(modelOrganism.getValues());
        assertEquals(3, modelOrganism.getValues().size());

        FacetItem itemValue = modelOrganism.getValues().get(0);
        assertNotNull(itemValue);
        assertNull(itemValue.getLabel());
        assertEquals("Human", itemValue.getValue());
        assertEquals(Long.valueOf(11L), itemValue.getCount());
    }

    @Test
    void convertFacetWithoutLabelDescendingValueSort() {
        List<FacetField> fieldList = getFacetFields("annotation");

        when(queryResponse.getFacetFields()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse);
        assertNotNull(facets);

        Facet annotation = facets.get(0);
        assertEquals("Annotation", annotation.getLabel());
        assertEquals("annotation", annotation.getName());
        assertTrue(annotation.isAllowMultipleSelection());
        assertNotNull(annotation.getValues());
        assertEquals(5, annotation.getValues().size());

        FacetItem itemValue = annotation.getValues().get(0);
        assertNotNull(itemValue);
        assertNull(itemValue.getLabel());
        assertEquals("5", itemValue.getValue());
        assertEquals(Long.valueOf(51L), itemValue.getCount());

        itemValue = annotation.getValues().get(1);
        assertNotNull(itemValue);
        assertNull(itemValue.getLabel());
        assertEquals("4", itemValue.getValue());
        assertEquals(Long.valueOf(41L), itemValue.getCount());
    }

    @Test
    void convertIntervalFacet() throws Exception {
        IntervalFacet intervalFacet = getLengthIntervalFacet();
        when(queryResponse.getIntervalFacets())
                .thenReturn(Collections.singletonList(intervalFacet));

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse);
        assertNotNull(facets);

        Facet lengthFacet = facets.get(0);
        assertEquals("Sequence Length", lengthFacet.getLabel());
        assertEquals("length", lengthFacet.getName());
        assertFalse(lengthFacet.isAllowMultipleSelection());
        assertNotNull(lengthFacet.getValues());
        assertEquals(3, lengthFacet.getValues().size());

        FacetItem itemValue = lengthFacet.getValues().get(0);
        assertNotNull(itemValue);
        assertEquals("1 - 200", itemValue.getLabel());
        assertEquals("[1 TO 200]", itemValue.getValue());
        assertEquals(Long.valueOf(10L), itemValue.getCount());
    }

    private List<FacetField> getFacetFields(String... name) {
        List<FacetField> fieldList = new ArrayList<>(1);
        if (Arrays.binarySearch(name, "reviewed") >= 0) {
            FacetField ffield = new FacetField("reviewed");
            ffield.add("true", 10L);
            ffield.add("false", 20L);
            fieldList.add(ffield);
        }
        if (Arrays.binarySearch(name, "fragment") >= 0) {
            FacetField ffield = new FacetField("fragment");
            ffield.add("false", 21L);
            ffield.add("true", 11L);
            fieldList.add(ffield);
        }
        if (Arrays.binarySearch(name, "model_organism") >= 0) {
            FacetField ffield = new FacetField("model_organism");
            ffield.add("Human", 11L);
            ffield.add("Mouse", 21L);
            ffield.add("Rat", 31L);
            fieldList.add(ffield);
        }
        if (Arrays.binarySearch(name, "annotation") >= 0) {
            FacetField ffield = new FacetField("annotation");
            ffield.add("1", 11L);
            ffield.add("2", 21L);
            ffield.add("3", 31L);
            ffield.add("4", 41L);
            ffield.add("5", 51L);
            fieldList.add(ffield);
        }
        return fieldList;
    }

    private IntervalFacet getLengthIntervalFacet() throws Exception {
        List<IntervalFacet.Count> counts = new ArrayList<>();
        Constructor countConstructor =
                Class.forName("org.apache.solr.client.solrj.response.IntervalFacet$Count")
                        .getDeclaredConstructor(String.class, Integer.TYPE);
        countConstructor.setAccessible(true);
        counts.add((IntervalFacet.Count) countConstructor.newInstance("[1,200]", 10));
        counts.add((IntervalFacet.Count) countConstructor.newInstance("[201,400]", 15));
        counts.add((IntervalFacet.Count) countConstructor.newInstance("[801,*]", 20));

        Constructor constructor =
                Class.forName("org.apache.solr.client.solrj.response.IntervalFacet")
                        .getDeclaredConstructor(String.class, List.class);
        constructor.setAccessible(true);
        return (IntervalFacet) constructor.newInstance("length", counts);
    }
}
