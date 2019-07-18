package uk.ac.ebi.uniprot.api.common.repository.search.facet;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.IntervalFacet;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author lgonzales
 */
class FacetConfigConverterTest {

    private QueryResponse queryResponse = mock(QueryResponse.class);

    @Test
    void convertFacetForNullResponse() {
        when(queryResponse.getFacetFields()).thenReturn(null);

        FakeFacetConfigConverter fakeFacetConfigConverter = new FakeFacetConfigConverter();
        List<Facet> facets = fakeFacetConfigConverter.convert(queryResponse);
        assertNotNull(facets);
        assertEquals(0,facets.size());
    }


    @Test
    void convertFacetWithLabel() {
        List<FacetField> fieldList = getFacetFields("fragment","reviewed");

        when(queryResponse.getFacetFields()).thenReturn(fieldList);

        FakeFacetConfigConverter fakeFacetConfigConverter = new FakeFacetConfigConverter();
        List<Facet> facets = fakeFacetConfigConverter.convert(queryResponse);
        assertNotNull(facets);
        assertEquals(2,facets.size());

        Facet reviewed = facets.get(0);
        assertEquals("Status",reviewed.getLabel());
        assertEquals("reviewed",reviewed.getName());
        assertFalse(reviewed.isAllowMultipleSelection());
        assertNotNull(reviewed.getValues());
        assertEquals(2,reviewed.getValues().size());

        FacetItem itemValue = reviewed.getValues().get(0);
        assertNotNull(itemValue);
        assertEquals("Reviewed (Swiss-Prot)",itemValue.getLabel());
        assertEquals("true",itemValue.getValue());
        assertEquals(new Long(10L),itemValue.getCount());
    }

    @Test
    void convertFacetWithoutLabel() {
        List<FacetField> fieldList = getFacetFields("popular_organism");

        when(queryResponse.getFacetFields()).thenReturn(fieldList);

        FakeFacetConfigConverter fakeFacetConfigConverter = new FakeFacetConfigConverter();
        List<Facet> facets = fakeFacetConfigConverter.convert(queryResponse);
        assertNotNull(facets);

        Facet popularOrganism = facets.get(0);
        assertEquals("Popular organisms",popularOrganism.getLabel());
        assertEquals("popular_organism",popularOrganism.getName());
        assertTrue(popularOrganism.isAllowMultipleSelection());
        assertNotNull(popularOrganism.getValues());
        assertEquals(3,popularOrganism.getValues().size());

        FacetItem itemValue = popularOrganism.getValues().get(0);
        assertNotNull(itemValue);
        assertNull(itemValue.getLabel());
        assertEquals("Human",itemValue.getValue());
        assertEquals(new Long(11L),itemValue.getCount());

    }

    @Test
    void convertIntervalFacet() throws Exception {
        IntervalFacet intervalFacet = getLengthIntervalFacet();
        when(queryResponse.getIntervalFacets()).thenReturn(Collections.singletonList(intervalFacet));

        FakeFacetConfigConverter fakeFacetConfigConverter = new FakeFacetConfigConverter();
        List<Facet> facets = fakeFacetConfigConverter.convert(queryResponse);
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
        assertEquals(new Long(10L), itemValue.getCount());
    }

    private List<FacetField> getFacetFields(String ... name) {
        List<FacetField> fieldList = new ArrayList<>(1);
        if(Arrays.binarySearch(name,"reviewed") >= 0) {
            FacetField ffield = new FacetField("reviewed");
            ffield.add("true", 10L);
            ffield.add("false", 20L);
            fieldList.add(ffield);
        }
        if(Arrays.binarySearch(name,"fragment") >= 0) {
            FacetField ffield = new FacetField("fragment");
            ffield.add("false", 21L);
            ffield.add("true", 11L);
            fieldList.add(ffield);
        }
        if(Arrays.binarySearch(name,"popular_organism") >= 0) {
            FacetField ffield = new FacetField("popular_organism");
            ffield.add("Human", 11L);
            ffield.add("Mouse", 21L);
            ffield.add("Rat", 31L);
            fieldList.add(ffield);
        }
        return fieldList;
    }

    private IntervalFacet getLengthIntervalFacet() throws Exception {
        List<IntervalFacet.Count> counts = new ArrayList<>();
        Constructor countConstructor = Class.forName("org.apache.solr.client.solrj.response.IntervalFacet$Count")
                .getDeclaredConstructor(String.class, Integer.TYPE);
        countConstructor.setAccessible(true);
        counts.add((IntervalFacet.Count) countConstructor.newInstance("[1,200]", 10));
        counts.add((IntervalFacet.Count) countConstructor.newInstance("[201,400]", 15));
        counts.add((IntervalFacet.Count) countConstructor.newInstance("[801,*]", 20));

        Constructor constructor = Class.forName("org.apache.solr.client.solrj.response.IntervalFacet")
                .getDeclaredConstructor(String.class, List.class);
        constructor.setAccessible(true);
        return (IntervalFacet) constructor.newInstance("length", counts);
    }

}