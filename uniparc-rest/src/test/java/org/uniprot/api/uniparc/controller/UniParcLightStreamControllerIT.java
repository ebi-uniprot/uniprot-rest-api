package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.uniparc.controller.UniParcITUtils.getUniParcDocument;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.convertToUniParcEntryLight;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 * @since 15/06/2020
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcEntryLightController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcLightStreamControllerIT extends AbstractStreamControllerIT {
    private static final String UPI_PREF = "UPI0000283A";
    private static final String UP_ID = "UP000005640";
    private static final String streamRequestPath = "/uniparc/stream";
    private static final String streamByProteomeIdRequestPath = "/uniparc/proteome/{upId}/stream";
    private static final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @Autowired private UniProtStoreClient<UniParcEntryLight> storeClient;
    @Autowired private UniParcCrossReferenceStoreClient xRefStoreClient;
    @Autowired protected MockMvc mockMvc;
    @Autowired private SolrClient solrClient;

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();

        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @Test
    void streamRdfCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE)
                        .param("query", "*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().string(startsWith(RdfPrologs.UNIPARC_PROLOG)))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n\n"
                                                        + "</rdf:RDF>")));
    }

    @Test
    void streamCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                containsInAnyOrder(
                                        "UPI0000283A10",
                                        "UPI0000283A09",
                                        "UPI0000283A08",
                                        "UPI0000283A07",
                                        "UPI0000283A06",
                                        "UPI0000283A05",
                                        "UPI0000283A04",
                                        "UPI0000283A03",
                                        "UPI0000283A02",
                                        "UPI0000283A01")));
    }

    @Test
    void streamBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(streamRequestPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "invalid:invalid")
                                .param("fields", "invalid,invalid1")
                                .param("sort", "invalid")
                                .param("download", "invalid"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'invalid' is not a valid search field",
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid1'",
                                        "Invalid sort parameter format. Expected format fieldName asc|desc.",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.")));
    }

    @Test
    void streamDownloadCompressedFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniparc_")))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", "upi desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains(
                                        "UPI0000283A10",
                                        "UPI0000283A09",
                                        "UPI0000283A08",
                                        "UPI0000283A07",
                                        "UPI0000283A06",
                                        "UPI0000283A05",
                                        "UPI0000283A04",
                                        "UPI0000283A03",
                                        "UPI0000283A02",
                                        "UPI0000283A01")));
    }

    @Test
    void streamDefaultSearchWithLowercaseId() throws Exception {

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "upi0000283A10");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.uniParcId", contains("UPI0000283A10")));
    }

    @ParameterizedTest(name = "[{index}] sort fieldName {0}")
    @MethodSource("getAllSortFields")
    void streamCanSortAllPossibleSortFields(String sortField) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", sortField + " asc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "uniprotkb:P10006 OR uniprotkb:P10005")
                        .param("fields", "gene,organism_id");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                containsInAnyOrder("UPI0000283A06", "UPI0000283A05")))
                .andExpect(
                        jsonPath(
                                "$.results.*.geneNames.*",
                                containsInAnyOrder("geneName05", "geneName06")))
                .andExpect(
                        jsonPath(
                                "$.results.*.organisms.*.taxonId",
                                containsInAnyOrder(9606, 7787, 9606, 7787)))
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.sequenceFeatures").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypesForUniParcStream")
    void streamAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath).header(ACCEPT, mediaType).param("query", "*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Test
    @Disabled
    void streamByProteomeIdCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                containsInAnyOrder(
                                        "UPI0000283A10",
                                        "UPI0000283A09",
                                        "UPI0000283A08",
                                        "UPI0000283A07",
                                        "UPI0000283A06",
                                        "UPI0000283A05",
                                        "UPI0000283A04",
                                        "UPI0000283A03",
                                        "UPI0000283A02",
                                        "UPI0000283A01")));
    }

    @Test
    @Disabled
    void streamByProteomeIdBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(streamByProteomeIdRequestPath, UP_ID)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "invalid,invalid1")
                                .param("sort", "invalid")
                                .param("download", "invalid"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid1'",
                                        "Invalid sort parameter format. Expected format fieldName asc|desc.",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.")));
    }

    @Test
    @Disabled
    void streamByProteomeIdyPDownloadCompressedFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniparc_")))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    @Disabled
    void streamByProteomeIdSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("sort", "upi desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains(
                                        "UPI0000283A10",
                                        "UPI0000283A09",
                                        "UPI0000283A08",
                                        "UPI0000283A07",
                                        "UPI0000283A06",
                                        "UPI0000283A05",
                                        "UPI0000283A04",
                                        "UPI0000283A03",
                                        "UPI0000283A02",
                                        "UPI0000283A01")));
    }

    @Test
    @Disabled
    void streamByProteomeIdDefaultSearchWithLowerCaseId() throws Exception {

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID.toLowerCase())
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @ParameterizedTest(name = "[{index}] sort fieldName {0}")
    @MethodSource("getAllSortFields")
    @Disabled
    void streamByProteomeIdCanSortAllPossibleSortFields(String sortField) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("sort", sortField + " asc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    @Disabled
    void streamByProteomeIdFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "gene,organism_id");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                hasItems("UPI0000283A01", "UPI0000283A02")))
                .andExpect(jsonPath("$.results.*.uniParcCrossReferences.*.geneName", empty()))
                .andExpect(
                        jsonPath("$.results.*.uniParcCrossReferences.*.organism.taxonId", empty()))
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.sequenceFeatures").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypesForUniParcStreamByByProteomeId")
    @Disabled
    void streamByProteomeIdAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamByProteomeIdRequestPath, UP_ID).header(ACCEPT, mediaType);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveEntry(i);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private void saveEntry(int i) throws Exception {
        UniParcEntry entry = UniParcEntryMocker.createUniParcEntry(i, UPI_PREF);
        UniParcDocument.UniParcDocumentBuilder builder = getUniParcDocument(entry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), builder.build());

        UniParcEntryLight entryLight = convertToUniParcEntryLight(entry);
        storeClient.saveEntry(entryLight);

        List<UniParcCrossReferencePair> xrefPairs =
                UniParcCrossReferenceMocker.createCrossReferencePairsFromXRefs(
                        entryLight.getUniParcId(),
                        xrefGroupSize,
                        entry.getUniParcCrossReferences());
        for (UniParcCrossReferencePair xrefPair : xrefPairs) {
            xRefStoreClient.saveEntry(xrefPair);
        }
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    private Stream<Arguments> getContentTypesForUniParcStream() {
        return super.getContentTypes(streamRequestPath);
    }

    private Stream<Arguments> getContentTypesForUniParcStreamByByProteomeId() {
        return super.getContentTypes(streamByProteomeIdRequestPath);
    }
}
