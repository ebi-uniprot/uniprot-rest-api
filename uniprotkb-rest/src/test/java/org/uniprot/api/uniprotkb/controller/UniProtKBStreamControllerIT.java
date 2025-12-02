package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 22/06/2020
 *
 * @author Edd
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest({UniProtKBController.class})
@ContextConfiguration(
        classes = {
            UniProtKBDataStoreTestConfig.class,
            UniProtKBREST.class,
            ErrorHandlerConfig.class
        })
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
            new UniProtEntryConverter(new HashMap<>(), new HashMap<>());
    @Autowired UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;
    @Autowired private MockMvc mockMvc;

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate uniProtRdfRestTemplate;

    @Autowired
    @Qualifier("uniProtKBSolrClient")
    private SolrClient solrClient;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private TaxonomyLineageRepository taxRepository;

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

        ReflectionTestUtils.setField(taxRepository, "solrClient", cloudSolrClient);
    }

    @BeforeEach
    void setUp() throws Exception {
        DefaultUriBuilderFactory handler = Mockito.mock(DefaultUriBuilderFactory.class);
        when(uniProtRdfRestTemplate.getUriTemplateHandler()).thenReturn(handler);

        UriBuilder uriBuilder = Mockito.mock(UriBuilder.class);
        when(handler.builder()).thenReturn(uriBuilder);

        URI uniProtRdfServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniprotkb"), eq("ttl"), any())).thenReturn(uniProtRdfServiceURI);
        when(uniProtRdfRestTemplate.getForObject(eq(uniProtRdfServiceURI), any()))
                .thenReturn(SAMPLE_TTL);
    }

    @Test
    void streamCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder(
                                        "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                        "P00007", "P00008", "P00009", "P00010")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CACHE_CONTROL, ControllerITUtils.CACHE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .stringValues(
                                        HttpHeaders.VARY,
                                        HttpHeaders.ACCEPT,
                                        HttpHeaders.ACCEPT_ENCODING,
                                        HttpCommonHeaderConfig.X_UNIPROT_RELEASE,
                                        HttpCommonHeaderConfig.X_API_DEPLOYMENT_DATE));
    }

    @Test
    void streamCanReturnLowercaseAccessionSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "p00001");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession", containsInAnyOrder("P00001")));
    }

    @Test
    void streamCanReturnIncludeIsoforms() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("includeIsoform", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(12)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
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
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "(content:BEK) AND (is_isoform:true)");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P00011-2", "P00012-2")));
    }

    @Test
    void streamCanReturnLineageData() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*:*")
                        .param("fields", "accession,lineage_ids");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.header().doesNotExist("Content-Disposition"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder(
                                        "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                        "P00007", "P00008", "P00009", "P00010")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.lineages[0].taxonId",
                                contains(
                                        9607, 9607, 9607, 9607, 9607, 9607, 9607, 9607, 9607,
                                        9607)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.lineages[1].taxonId",
                                contains(
                                        9608, 9608, 9608, 9608, 9608, 9608, 9608, 9608, 9608,
                                        9608)));
    }

    @Test
    void streamBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(streamRequestPath)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "invalid:invalid")
                                .param("fields", "invalid,invalid1")
                                .param("sort", "invalid")
                                .param("download", "invalid"));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
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
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprotkb_")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("sort", "accession desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
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
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("sort", sortField + " asc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "accession:P00006 OR accession:P00005")
                        .param("fields", "gene_primary");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.primaryAccession",
                                containsInAnyOrder("P00005", "P00006")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results.*.genes.*.geneName.*", contains("FGFR2", "FGFR2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.*.keywords").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.*.sequence").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void streamAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, mediaType)
                        .param("query", "content:*")
                        .param("fields", "accession,rhea");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(mediaType));
    }

    @Test
    void streamTSVFormatWithRheaId() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE)
                        .param("query", "content:*")
                        .param("fields", "accession,rhea");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .contentTypeCompatibleWith(UniProtMediaType.TSV_MEDIA_TYPE))
                .andExpect(MockMvcResultMatchers.content().string(containsString("Entry\tRhea ID")))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(not(containsString("RHEA-COMP:10136 RHEA-COMP:10137"))))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(containsString("P00001\tRHEA:10596")));
    }

    @Test
    void streamTooManyEntriesResponseError() throws Exception {
        for (int i = 13; i <= 20; i++) {
            saveEntry(i, "");
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(response))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.FORBIDDEN.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.url", is("http://localhost/uniprotkb/stream")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages.*",
                                contains(
                                        "Too many results to retrieve. Please refine your query or consider fetching batch by batch")));
    }

    @Test
    void testStreamWithMoreThanAllowedOrClausesFailure() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.get(streamRequestPath)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                        .param(
                                "query",
                                "accession:P21802 OR accession:P00001 OR accession:P00002 OR accession:P00003 OR accession:P00004 OR accession:P00005 OR accession:P00006 OR accession:P00007 OR accession:P00008 OR accession:P00009 OR accession:P00010 OR accession:P00011 OR accession:P00012 OR accession:P00013 OR accession:P00014 OR accession:P00015 OR accession:P00016 OR accession:P00017 OR accession:P00018 OR accession:P00019 OR accession:P00020 OR accession:P00021 OR accession:P00022 OR accession:P00023 OR accession:P00024 OR accession:P00025 OR accession:P00026 OR accession:P00027 OR accession:P00028 OR accession:P00029 OR accession:P00030 OR accession:P00031 OR accession:P00032 OR accession:P00033 OR accession:P00034 OR accession:P00035 OR accession:P00036 OR accession:P00037 OR accession:P00038 OR accession:P00039 OR accession:P00040 OR accession:P00041 OR accession:P00042 OR accession:P00043 OR accession:P00044 OR accession:P00045 OR accession:P00046 OR accession:P00047 OR accession:P00048 OR accession:P00049 OR accession:P00050");

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.url")
                                .value("http://localhost/uniprotkb/stream"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value("Too many OR conditions in query. Maximum allowed is 50."));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniprot, SolrCollection.taxonomy);
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

        saveTaxonomyEntry(9606L);
        cloudSolrClient.commit(SolrCollection.taxonomy.name());
    }

    private void saveTaxonomyEntry(long taxId) throws Exception {
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .rank(TaxonomyRank.SPECIES)
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 1).build())
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 2).build())
                        .build();
        byte[] taxonomyObj =
                TaxonomyJsonConfig.getInstance()
                        .getFullObjectMapper()
                        .writeValueAsBytes(taxonomyEntry);

        TaxonomyDocument.TaxonomyDocumentBuilder docBuilder =
                TaxonomyDocument.builder()
                        .taxId(taxId)
                        .id(String.valueOf(taxId))
                        .taxonomyObj(taxonomyObj);
        cloudSolrClient.addBean(SolrCollection.taxonomy.name(), docBuilder.build());
    }

    private void saveEntry(int i, String isoFormString) throws Exception {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("P%05d", i) + isoFormString;
        entryBuilder.primaryAccession(acc);

        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

        cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
        uniProtKBStoreClient.saveEntry(uniProtKBEntry);
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
