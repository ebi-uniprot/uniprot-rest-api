package org.uniprot.api.common.repository.search.facet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.json.NestableJsonFacet;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.Test;

/**
 * @author lgonzales
 */
class FacetResponseConverterTest {

    private final QueryResponse queryResponse = mock(QueryResponse.class);

    @Test
    void convertFacetForNullResponse() {
        when(queryResponse.getFacetFields()).thenReturn(null);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse, new ArrayList<>());
        assertNotNull(facets);
        assertEquals(0, facets.size());
    }

    @Test
    void convertFacetWithLabel() {
        NestableJsonFacet fieldList = getFacetFields("fragment", "reviewed");

        when(queryResponse.getJsonFacetingResponse()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse, List.of("reviewed", "fragment"));
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
        NestableJsonFacet fieldList = getFacetFields("model_organism");

        when(queryResponse.getJsonFacetingResponse()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse, List.of("model_organism"));
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
        NestableJsonFacet fieldList = getFacetFields("annotation");

        when(queryResponse.getJsonFacetingResponse()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse, List.of("annotation"));
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
        NestableJsonFacet fieldList = getFacetFields("length");

        when(queryResponse.getJsonFacetingResponse()).thenReturn(fieldList);

        FacetResponseConverter facetConverter = new FacetResponseConverter(new FakeFacetConfig());
        List<Facet> facets = facetConverter.convert(queryResponse, List.of("length"));
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

    private NestableJsonFacet getFacetFields(String... name) {
        NamedList<Object> fieldList = new NamedList();
        if (Arrays.binarySearch(name, "reviewed") >= 0) {
            List<NamedList> values = new ArrayList<>();

            NamedList<Object> item = new NamedList();
            item.add("val", true);
            item.add("count", 10L);
            values.add(item);

            item = new NamedList();
            item.add("val", false);
            item.add("count", 20L);
            values.add(item);

            NamedList<Object> bucketsItems = new NamedList();
            bucketsItems.add("buckets", values);

            fieldList.add("reviewed", bucketsItems);
        }
        if (Arrays.binarySearch(name, "fragment") >= 0) {
            List<NamedList> values = new ArrayList<>();
            NamedList<Object> item = new NamedList();

            item.add("val", false);
            item.add("count", 21L);
            values.add(item);

            item = new NamedList();
            item.add("val", true);
            item.add("count", 11L);
            values.add(item);

            NamedList<Object> bucketsItems = new NamedList();
            bucketsItems.add("buckets", values);

            fieldList.add("fragment", bucketsItems);
        }
        if (Arrays.binarySearch(name, "model_organism") >= 0) {
            List<NamedList> values = new ArrayList<>();
            NamedList<Object> item = new NamedList();

            item.add("val", "Human");
            item.add("count", 11L);
            values.add(item);

            item = new NamedList();
            item.add("val", "Mouse");
            item.add("count", 21L);
            values.add(item);

            item = new NamedList();
            item.add("val", "Rat");
            item.add("count", 31L);
            values.add(item);

            NamedList<Object> bucketsItems = new NamedList();
            bucketsItems.add("buckets", values);

            fieldList.add("model_organism", bucketsItems);
        }
        if (Arrays.binarySearch(name, "annotation") >= 0) {

            List<NamedList> values = new ArrayList<>();
            NamedList<Object> item = new NamedList();

            item = new NamedList();
            item.add("val", 5);
            item.add("count", 51L);
            values.add(item);

            item = new NamedList();
            item.add("val", 4);
            item.add("count", 41L);
            values.add(item);

            item = new NamedList();
            item.add("val", 3);
            item.add("count", 31L);
            values.add(item);

            item = new NamedList();
            item.add("val", 2);
            item.add("count", 21L);
            values.add(item);

            item.add("val", 1);
            item.add("count", 11L);
            values.add(item);

            NamedList<Object> bucketsItems = new NamedList();
            bucketsItems.add("buckets", values);

            fieldList.add("annotation", bucketsItems);
        }

        if (Arrays.binarySearch(name, "length") >= 0) {

            List<NamedList> values = new ArrayList<>();
            NamedList<Object> item = new NamedList();

            item = new NamedList();
            item.add("val", "[1,200]");
            item.add("count", 10L);
            values.add(item);

            item = new NamedList();
            item.add("val", "[201,400]");
            item.add("count", 15L);
            values.add(item);

            item = new NamedList();
            item.add("val", "[801,*]");
            item.add("count", 20L);
            values.add(item);

            NamedList<Object> bucketsItems = new NamedList();
            bucketsItems.add("buckets", values);

            fieldList.add("length", bucketsItems);
        }
        return new NestableJsonFacet(fieldList);
    }
}
