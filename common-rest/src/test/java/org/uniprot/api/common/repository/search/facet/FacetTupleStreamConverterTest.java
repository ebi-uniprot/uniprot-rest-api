package org.uniprot.api.common.repository.search.facet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.jupiter.api.Test;

class FacetTupleStreamConverterTest {

    private final TupleStream tupleStream = mock(TupleStream.class);
    private final String idFieldName = "accession_id";

    @Test
    void convertFacetForNullResponse() throws IOException {
        Tuple tuple = new Tuple();
        tuple.EOF = true;
        when(tupleStream.read()).thenReturn(tuple);
        FacetTupleStreamConverter facetConverter =
                new FacetTupleStreamConverter(idFieldName, new FakeFacetConfig());
        SolrStreamFacetResponse response = facetConverter.convert(tupleStream, null);
        assertNotNull(response);
        assertNotNull(response.getFacets());
        assertEquals(0, response.getFacets().size());
        assertNotNull(response.getIds());
        assertEquals(0, response.getIds().size());
    }

    @Test
    void convertFacetWithLabel() throws IOException {
        Tuple tuple1 = getTuple("reviewed", true, 234L);
        Tuple tuple2 = getTuple("reviewed", false, 24L);
        Tuple tuple3 = getTuple("model_organism", "Human", 123L);
        Tuple tuple4 = getTuple("model_organism", "Rice", 12L);
        Tuple tuple5 = getTuple("model_organism", "Rice 1", 11L);
        Tuple tuple6 = getTuple("model_organism", "Rice 2", 10L);
        Tuple tuple7 = getTuple("model_organism", "Rice 3", 9L);
        Tuple tuple8 = getTuple("model_organism", "Rice 4", 8L);
        Tuple eofTuple = new Tuple();
        eofTuple.EOF = true;

        when(tupleStream.read())
                .thenReturn(
                        tuple1, tuple2, tuple3, tuple4, tuple5, tuple6, tuple7, tuple8, eofTuple);

        FacetTupleStreamConverter facetConverter =
                new FacetTupleStreamConverter(idFieldName, new FakeFacetConfig());
        SolrStreamFacetResponse response = facetConverter.convert(tupleStream, null);
        assertNotNull(response);
        List<Facet> facets = response.getFacets();
        assertNotNull(response.getIds());
        assertEquals(0, response.getIds().size());
        assertNotNull(facets);
        assertEquals(2, facets.size());

        Facet statusFacet = facets.get(0);
        assertEquals("Status", statusFacet.getLabel());
        assertEquals("reviewed", statusFacet.getName());
        assertFalse(statusFacet.isAllowMultipleSelection());
        assertNotNull(statusFacet.getValues());
        assertEquals(2, statusFacet.getValues().size());

        FacetItem itemValue1 = statusFacet.getValues().get(0);
        assertNotNull(itemValue1);
        assertEquals("Reviewed (Swiss-Prot)", itemValue1.getLabel());
        assertEquals("true", itemValue1.getValue());
        assertEquals(Long.valueOf(234L), itemValue1.getCount());

        FacetItem itemValue2 = statusFacet.getValues().get(1);
        assertNotNull(itemValue1);
        assertEquals("Unreviewed (TrEMBL)", itemValue2.getLabel());
        assertEquals("false", itemValue2.getValue());
        assertEquals(Long.valueOf(24L), itemValue2.getCount());

        Facet organismFacet = facets.get(1);
        assertEquals("Model organisms", organismFacet.getLabel());
        assertEquals("model_organism", organismFacet.getName());
        assertTrue(organismFacet.isAllowMultipleSelection());
        assertNotNull(organismFacet.getValues());
        assertEquals(5, organismFacet.getValues().size());

        FacetItem itemValue3 = organismFacet.getValues().get(0);
        assertNotNull(itemValue3);
        assertNull(itemValue3.getLabel());
        assertEquals("Human", itemValue3.getValue());
        assertEquals(Long.valueOf(123L), itemValue3.getCount());

        FacetItem itemValue4 = organismFacet.getValues().get(1);
        assertNotNull(itemValue4);
        assertNull(itemValue4.getLabel());
        assertEquals("Rice", itemValue4.getValue());
        assertEquals(Long.valueOf(12L), itemValue4.getCount());
    }

    @Test
    void convertIntervalFacet() throws Exception {
        Tuple tuple1 = getTuple("length", 116, 2L);
        Tuple tuple2 = getTuple("length", 220, 2L);
        Tuple tuple3 = getTuple("length", 821, 1L);
        Tuple tuple4 = getTuple("length", 1047, 1L);
        Tuple tuple5 = getTuple("length", 1, 2L);
        Tuple eofTuple = new Tuple();
        eofTuple.EOF = true;

        when(tupleStream.read()).thenReturn(tuple1, tuple2, tuple3, tuple4, tuple5, eofTuple);

        FacetTupleStreamConverter facetConverter =
                new FacetTupleStreamConverter(idFieldName, new FakeFacetConfig());
        SolrStreamFacetResponse response = facetConverter.convert(tupleStream, null);

        List<Facet> facets = response.getFacets();
        assertNotNull(response.getIds());
        assertEquals(0, response.getIds().size());
        assertNotNull(facets);
        assertEquals(1, facets.size());
        Facet lengthFacet = facets.get(0);
        assertEquals("Sequence Length", lengthFacet.getLabel());
        assertEquals("length", lengthFacet.getName());
        assertFalse(lengthFacet.isAllowMultipleSelection());
        assertNotNull(lengthFacet.getValues());
        assertEquals(3, lengthFacet.getValues().size());

        FacetItem itemValue0 = lengthFacet.getValues().get(0);
        assertNotNull(itemValue0);
        assertEquals("1 - 200", itemValue0.getLabel());
        assertEquals("[1 TO 200]", itemValue0.getValue());
        assertEquals(Long.valueOf(4L), itemValue0.getCount());

        FacetItem itemValue1 = lengthFacet.getValues().get(1);
        assertNotNull(itemValue1);
        assertEquals("201 - 400", itemValue1.getLabel());
        assertEquals("[201 TO 400]", itemValue1.getValue());
        assertEquals(Long.valueOf(2L), itemValue1.getCount());

        FacetItem itemValue2 = lengthFacet.getValues().get(2);
        assertNotNull(itemValue2);
        assertEquals(">= 801", itemValue2.getLabel());
        assertEquals("[801 TO *]", itemValue2.getValue());
        assertEquals(Long.valueOf(2L), itemValue2.getCount());
    }

    @Test
    void convertFacetWithAccessions() throws IOException {
        Tuple tuple1 = getTuple("reviewed", true, 234L);
        Tuple tuple2 = getTuple("reviewed", false, 24L);
        Tuple tuple3 = getTuple("model_organism", "Human", 123L);
        Tuple tuple4 = getTuple("model_organism", "Rice", 12L);
        Tuple tuple5 = getTupleWithoutCount("accession_id", "P12345");
        Tuple tuple6 = getTupleWithoutCount("accession_id", "Q12345");
        Tuple eofTuple = new Tuple();
        eofTuple.EOF = true;

        when(tupleStream.read())
                .thenReturn(tuple1, tuple2, tuple3, tuple4, tuple5, tuple6, eofTuple);

        FacetTupleStreamConverter facetConverter =
                new FacetTupleStreamConverter(idFieldName, new FakeFacetConfig());
        SolrStreamFacetResponse response = facetConverter.convert(tupleStream, null);
        assertNotNull(response);
        List<Facet> facets = response.getFacets();
        assertNotNull(response.getIds());
        assertNotNull(facets);
        assertEquals(2, facets.size());
        assertEquals(2, response.getIds().size());
        assertEquals(Arrays.asList("P12345", "Q12345"), response.getIds());
    }

    private Tuple getTuple(String facet, Object value, Long count) {
        Tuple tuple = new Tuple();
        Map<Object, Object> map = new HashMap<>();
        map.put(facet, value);
        map.put("count(*)", count);
        tuple.fields = map;
        return tuple;
    }

    private Tuple getTupleWithoutCount(String facet, String accession) {
        Tuple tuple = new Tuple();
        Map<Object, Object> map = new HashMap<>();
        map.put(facet, accession);
        tuple.fields = map;
        return tuple;
    }
}
