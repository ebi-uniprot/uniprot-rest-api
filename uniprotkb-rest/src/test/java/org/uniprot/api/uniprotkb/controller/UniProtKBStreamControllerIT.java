package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Created 22/06/2020
 *
 * @author Edd
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest({UniProtKBController.class})
@ContextConfiguration(
        classes = {DataStoreTestConfig.class, UniProtKBREST.class, ErrorHandlerConfig.class})
@ExtendWith(
        value = {
            SpringExtension.class,
        })
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBStreamControllerIT extends AbstractStreamControllerIT {
    private static final String streamRequestPath = "/uniprotkb/stream";
    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    Mockito.mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());
    @Autowired UniProtStoreClient<UniProtKBEntry> storeClient;
    @Autowired private MockMvc mockMvc;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

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
        JsonQueryRequest jsonQueryRequest = mock(JsonQueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    @Test
    void streamCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder(
                                        "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                        "P00007", "P00008", "P00009", "P00010")));
    }

    @Test
    void streamCanReturnIncludeIsoforms() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("includeIsoform", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder(
                                        "P00001",
                                        "P00002",
                                        "P00003",
                                        "P00004",
                                        "P00005",
                                        "P00006",
                                        "P00007",
                                        "P00008",
                                        "P00009",
                                        "P00010",
                                        "P00011-2",
                                        "P00012-2")));
    }

    @Test
    void streamCanReturnIsoformsOnly() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "(content:BEK) AND (is_isoform:true)");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P00011-2", "P00012-2")));
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
                        .param("query", "content:*")
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
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("sort", "accession desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                contains(
                                        "P00010", "P00009", "P00008", "P00007", "P00006", "P00005",
                                        "P00004", "P00003", "P00002", "P00001")));
    }

    @ParameterizedTest(name = "[{index}] sort fieldName {0}")
    @MethodSource("getAllSortFields")
    void streamCanSortAllPossibleSortFields(String sortField) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
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
                        .param("query", "accession:P00006 OR accession:P00005")
                        .param("fields", "gene_primary");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P00005", "P00006")))
                .andExpect(jsonPath("$.results.*.genes.*.geneName.*", contains("FGFR2", "FGFR2")))
                .andExpect(jsonPath("$.results.*.keywords").doesNotExist())
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void streamAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, mediaType)
                        .param("query", "content:*")
                        .param("fields", "accession,rhea");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    @Test
    void streamTSVFormatWithRheaId() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE)
                        .param("query", "content:*")
                        .param("fields", "accession,rhea");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(content().contentTypeCompatibleWith(UniProtMediaType.TSV_MEDIA_TYPE))
                .andExpect(content().string(containsString("Entry\tRhea ID")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "P00001\tRHEA:10596 RHEA-COMP:10136 RHEA-COMP:10137")));
    }

    @Test
    void streamTooManyEntriesResponseError() throws Exception {
        for (int i = 13; i <= 20; i++) {
            saveEntry(i, "");
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", is("http://localhost/uniprotkb/stream")))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Too many results to retrieve. Please refine your query or consider fetching batch by batch")));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniprot);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveEntry(i, "");
        }
        saveEntry(11, "-2");
        saveEntry(12, "-2");
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    private void saveEntry(int i, String isoFormString) throws Exception {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("P%05d", i) + isoFormString;
        entryBuilder.primaryAccession(acc);

        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

        cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
        storeClient.saveEntry(uniProtKBEntry);
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    private Stream<Arguments> getContentTypes() {
        return super.getContentTypes(streamRequestPath);
    }
}
