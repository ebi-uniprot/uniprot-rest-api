package org.uniprot.api.mapto.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrFacetRequest;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;

@ExtendWith(MockitoExtension.class)
class UniRefMapToTargetServiceTest {
    @Mock private StoreStreamer<UniRefEntryLight> storeStreamer;
    @Mock private FacetTupleStreamTemplate tupleStreamTemplate;
    @Mock private FacetConfig facetConfig;
    @Mock private RequestConverter requestConverter;
    @Mock private MapToJobService mapToJobService;
    @Mock private RdfStreamer rdfStreamer;
    private UniRefMapToTargetService mapToTargetService;
    private String id1;
    private String id2;
    private String id3;
    private List<String> toIds;
    private UniRefEntryLight entry1;
    private UniRefEntryLight entry2;
    private UniRefEntryLight entry3;

    @BeforeEach
    void init() {
        this.mapToTargetService =
                new UniRefMapToTargetService(
                        storeStreamer,
                        tupleStreamTemplate,
                        facetConfig,
                        requestConverter,
                        mapToJobService,
                        rdfStreamer);
        ReflectionTestUtils.setField(
                this.mapToTargetService, "maxIdMappingToIdsCountEnriched", 100000);
        ReflectionTestUtils.setField(
                this.mapToTargetService, "maxIdMappingToIdsCountWithFacets", 25000);
        ReflectionTestUtils.setField(this.mapToTargetService, "idBatchSize", 5000);
        id1 = "UniRef100_A0A001";
        id2 = "UniRef100_A0A002";
        id3 = "UniRef100_A0A003";
        toIds = new ArrayList<>();
        toIds.add(id1);
        toIds.add(id2);
        toIds.add(id3);
        entry1 = new UniRefEntryLightBuilder().id(id1).build();
        entry2 = new UniRefEntryLightBuilder().id(id2).build();
        entry3 = new UniRefEntryLightBuilder().id(id3).build();
    }

    @Test
    void testGetMappedEntriesWithoutSolr() {
        // when
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setSize(10);
        when(this.storeStreamer.streamEntries(eq(toIds), any()))
                .thenReturn(Stream.of(entry1, entry2, entry3));
        // then
        QueryResult<UniRefEntryLight> result =
                this.mapToTargetService.getMappedEntries(searchRequest, toIds);
        assertNotNull(result);
        List<UniRefEntryLight> entries = result.getContent().collect(Collectors.toList());
        assertFalse(entries.isEmpty());
        assertEquals(3, entries.size());
        assertEquals(List.of(entry1, entry2, entry3), entries);
        assertNull(result.getFacets());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testGetMappedEntriesWithCursorWithoutSolr() {
        // when
        String id4 = "UniRef100_A0A004";
        String id5 = "UniRef100_A0A005";
        toIds.add(id4);
        toIds.add(id5);
        UniRefEntryLight entry4 = new UniRefEntryLightBuilder().id(id4).build();
        UniRefEntryLight entry5 = new UniRefEntryLightBuilder().id(id5).build();
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        // page size = 2 to test cursor
        searchRequest.setSize(2);
        when(this.storeStreamer.streamEntries(eq(List.of(id1, id2)), any()))
                .thenReturn(Stream.of(entry1, entry2));
        when(this.storeStreamer.streamEntries(eq(List.of(id3, id4)), any()))
                .thenReturn(Stream.of(entry3, entry4));
        when(this.storeStreamer.streamEntries(eq(List.of(id5)), any()))
                .thenReturn(Stream.of(entry5));
        // then
        // page 1
        QueryResult<UniRefEntryLight> result =
                this.mapToTargetService.getMappedEntries(searchRequest, toIds);
        assertNotNull(result);
        List<UniRefEntryLight> entries = result.getContent().collect(Collectors.toList());
        assertFalse(entries.isEmpty());
        assertEquals(2, entries.size());
        assertEquals(List.of(entry1, entry2), entries);
        assertNull(result.getFacets());
        assertTrue(result.getWarnings().isEmpty());
        // call with next cursor - page 2
        CursorPage cursorPage = (CursorPage) result.getPage();
        assertTrue(cursorPage.hasNextPage());
        String nextCursor = cursorPage.getEncryptedNextCursor();
        searchRequest.setCursor(nextCursor);
        QueryResult<UniRefEntryLight> result2 =
                this.mapToTargetService.getMappedEntries(searchRequest, toIds);
        assertNotNull(result2);
        List<UniRefEntryLight> entries2 = result2.getContent().collect(Collectors.toList());
        assertEquals(List.of(entry3, entry4), entries2);
        // next cursor - page 3
        CursorPage cursorPage2 = (CursorPage) result2.getPage();
        assertTrue(cursorPage2.hasNextPage());
        String nextCursor2 = cursorPage2.getEncryptedNextCursor();
        searchRequest.setCursor(nextCursor2);
        QueryResult<UniRefEntryLight> result3 =
                this.mapToTargetService.getMappedEntries(searchRequest, toIds);
        assertNotNull(result3);
        List<UniRefEntryLight> entries3 = result3.getContent().collect(Collectors.toList());
        assertEquals(List.of(entry5), entries3);
        // try to get cursor when none left
        CursorPage cursorPage3 = (CursorPage) result3.getPage();
        assertFalse(cursorPage3.hasNextPage());
    }

    @Test
    void testGetMappedEntriesWithSolr() throws IOException {
        // when
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setSize(10);
        searchRequest.setSort("id desc"); // sort by descending order
        SolrRequest solrRequest = mock(SolrRequest.class);
        when(this.requestConverter.createSearchIdsSolrRequest(
                        eq(searchRequest), eq(toIds), eq("id")))
                .thenReturn(solrRequest);
        when(solrRequest.createSearchRequest()).thenReturn(solrRequest);
        // create three mock tuples and map in descending id order
        Map<Object, Object> map1 = new HashMap<>();
        map1.put("id", "UniRef100_A0A003");
        Tuple tuple1 = new Tuple(map1);
        Map<Object, Object> map2 = new HashMap<>();
        map2.put("id", "UniRef100_A0A002");
        Tuple tuple2 = new Tuple(map2);
        Map<Object, Object> map3 = new HashMap<>();
        map3.put("id", "UniRef100_A0A001");
        Tuple tuple3 = new Tuple(map3);
        Map<Object, Object> map4 = new HashMap<>();
        map4.put("EOF", "");
        Tuple eofTuple = new Tuple(map4); // EOF tuple
        // Mock tupleStream.read() behavior
        TupleStream tupleStream = mock(TupleStream.class);
        when(this.tupleStreamTemplate.create(any())).thenReturn(tupleStream);
        doNothing().when(tupleStream).open();
        doNothing().when(tupleStream).close();
        when(tupleStream.read()).thenReturn(tuple1, tuple2, tuple3, eofTuple);
        when(this.storeStreamer.streamEntries(eq(List.of(id3, id2, id1)), any()))
                .thenReturn(Stream.of(entry3, entry2, entry1));
        // then
        QueryResult<UniRefEntryLight> result =
                this.mapToTargetService.getMappedEntries(searchRequest, toIds);
        assertNotNull(result);
        List<UniRefEntryLight> entries = result.getContent().collect(Collectors.toList());
        assertFalse(entries.isEmpty());
        assertEquals(3, entries.size());
        assertEquals(List.of(entry3, entry2, entry1), entries);
        assertTrue(result.getFacets().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testGetMappedEntriesWithFacet() throws IOException {
        // when
        String facetName = "identity";
        UniRefSearchRequest searchRequest = new UniRefSearchRequest();
        searchRequest.setSize(10);
        searchRequest.setFacets("identity"); // sort by descending order
        SolrRequest solrRequest = mock(SolrRequest.class);
        SolrFacetRequest facetRequest = new SolrFacetRequest("identity", 5, 5, "", Map.of());
        when(solrRequest.getFacets()).thenReturn(List.of(facetRequest));
        when(solrRequest.getIds()).thenReturn(List.of(id1, id2, id3));
        SolrRequest solrBatchRequest = mock(SolrRequest.class);
        when(solrRequest.createBatchFacetSolrRequest(any())).thenReturn(solrBatchRequest);
        when(solrBatchRequest.getFacets()).thenReturn(List.of(facetRequest));
        when(this.requestConverter.createSearchIdsSolrRequest(
                        eq(searchRequest), eq(toIds), eq("id")))
                .thenReturn(solrRequest);
        when(solrRequest.createSearchRequest()).thenReturn(solrRequest);
        // create mock tuples with facets
        Map<Object, Object> map1 = new HashMap<>();
        map1.put(facetName, "1.0");
        map1.put("count(*)", Long.valueOf(3));
        Tuple tuple1 = new Tuple(map1);
        Map<Object, Object> map2 = new HashMap<>();
        map2.put("EOF", "");
        Tuple eofTuple = new Tuple(map2); // EOF tuple
        TupleStream tupleStream = mock(TupleStream.class);
        when(this.tupleStreamTemplate.create(any())).thenReturn(tupleStream);
        doNothing().when(tupleStream).open();
        doNothing().when(tupleStream).close();
        FacetProperty facetProperty = new FacetProperty();
        facetProperty.setLabel("Clusters");
        facetProperty.setAllowmultipleselection(true);
        AtomicInteger callCount = new AtomicInteger(0);
        when(facetConfig.getFacetPropertyMap()).thenReturn(Map.of(facetName, facetProperty));
        when(tupleStream.read())
                .thenAnswer(
                        invocation -> {
                            int count = callCount.getAndIncrement();
                            if (count == 1) { // First and second calls return tuple1
                                return tuple1;
                            }
                            return eofTuple; // Third and fourth calls return eofTuple
                        });
        // then
        QueryResult<UniRefEntryLight> result =
                this.mapToTargetService.getMappedEntries(searchRequest, toIds);
        assertNotNull(result);
        List<UniRefEntryLight> entries = result.getContent().collect(Collectors.toList());
        assertTrue(entries.isEmpty());
        assertEquals(1, result.getFacets().size());
        Facet facet = result.getFacets().stream().toList().get(0);
        assertEquals(facetName, facet.getName());
        assertEquals(1, facet.getValues().size());
        FacetItem facetItem = facet.getValues().get(0);
        assertEquals("1.0", facetItem.getValue());
        assertEquals(3L, facetItem.getCount());
    }

    @Test
    void testStreamEntriesWithoutSolr() {
        // when
        UniRefStreamRequest streamRequest = new UniRefStreamRequest();
        when(this.storeStreamer.streamEntries(eq(toIds), any()))
                .thenReturn(Stream.of(entry1, entry2, entry3));
        // then
        Stream<UniRefEntryLight> result =
                this.mapToTargetService.streamEntries(streamRequest, toIds);
        assertNotNull(result);
        List<UniRefEntryLight> entries = result.toList();
        assertEquals(3, entries.size());
        assertEquals(List.of(entry1, entry2, entry3), entries);
    }

    @Test
    void testStreamEntriesWithSolr() throws IOException {
        // when
        UniRefStreamRequest streamRequest = new UniRefStreamRequest();
        streamRequest.setSort("id desc"); // sort by descending order
        SolrRequest solrRequest = mock(SolrRequest.class);
        when(this.requestConverter.createStreamIdsSolrRequest(
                        eq(streamRequest), eq(toIds), eq("id")))
                .thenReturn(solrRequest);
        when(solrRequest.createSearchRequest()).thenReturn(solrRequest);
        // create three mock tuples and map in descending id order
        Map<Object, Object> map1 = new HashMap<>();
        map1.put("id", "UniRef100_A0A003");
        Tuple tuple1 = new Tuple(map1);
        Map<Object, Object> map2 = new HashMap<>();
        map2.put("id", "UniRef100_A0A002");
        Tuple tuple2 = new Tuple(map2);
        Map<Object, Object> map3 = new HashMap<>();
        map3.put("id", "UniRef100_A0A001");
        Tuple tuple3 = new Tuple(map3);
        Map<Object, Object> map4 = new HashMap<>();
        map4.put("EOF", "");
        Tuple eofTuple = new Tuple(map4); // EOF tuple
        // Mock tupleStream.read() behavior
        TupleStream tupleStream = mock(TupleStream.class);
        when(this.tupleStreamTemplate.create(any())).thenReturn(tupleStream);
        doNothing().when(tupleStream).open();
        doNothing().when(tupleStream).close();
        when(tupleStream.read()).thenReturn(tuple1, tuple2, tuple3, eofTuple);
        when(this.storeStreamer.streamEntries(eq(List.of(id3, id2, id1)), any()))
                .thenReturn(Stream.of(entry3, entry2, entry1));
        // then
        Stream<UniRefEntryLight> result =
                this.mapToTargetService.streamEntries(streamRequest, toIds);
        assertNotNull(result);
        List<UniRefEntryLight> entries = result.toList();
        assertEquals(3, entries.size());
        assertEquals(List.of(entry3, entry2, entry1), entries);
    }
}
