package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.createEntry;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.common.repository.UniRefDataStoreTestConfig;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 08/01/2021
 */
@ContextConfiguration(
        classes = {
            UniRefDataStoreTestConfig.class,
            UniRefRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefEntryLightController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
class UniRefLightGetIdControllerIT {
    private static final String ID = "UniRef50_P03901";
    private static final String NAME = "Cluster: MoeK5 01";

    @Autowired private UniRefQueryRepository repository;

    @Autowired private UniRefLightStoreClient lightStoreClient;

    @MockBean(name = "uniRefRdfRestTemplate")
    private RestTemplate restTemplate;

    private static final String ID_LIGHT_PREFIX_PATH = "/uniref/";

    private static final String ID_LIGHT_SUFIX_PATH = "/light";

    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initDataStore() {
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIREF_LIGHT, SolrCollection.uniref);
        storeManager.addStore(DataStoreManager.StoreType.UNIREF_LIGHT, lightStoreClient);
        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIREF_LIGHT,
                new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo()));

        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIREF_LIGHT));
        saveEntry();
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @AfterAll
    void cleanData() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIREF_LIGHT);
        storeManager.cleanStore(DataStoreManager.StoreType.UNIREF_LIGHT);
    }

    protected void saveEntry() {
        UniRefEntry unirefEntry = createEntry(1, UniRefType.UniRef50);
        saveEntry(unirefEntry);
    }

    private void saveEntry(UniRefEntry unirefEntry) {
        UniRefEntryConverter converter = new UniRefEntryConverter();
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        Entry entry = converter.toXml(unirefEntry);
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(entry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIREF_LIGHT, entry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIREF_LIGHT, entryLight);
    }

    @Test
    void validIdReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", is(ID)))
                .andExpect(jsonPath("$.name", is("Cluster: MoeK5 01")))
                .andExpect(jsonPath("$.commonTaxon.taxonId", is(9606)))
                .andExpect(jsonPath("$.commonTaxon.scientificName", is("Homo sapiens")))
                .andExpect(jsonPath("$.seedId", is("P12301")))
                .andExpect(jsonPath("$.goTerms[0].goId", is("GO:0044444")))
                .andExpect(jsonPath("$.representativeMember.accessions[0]", is("P12301")))
                .andExpect(jsonPath("$.representativeMember.memberId", is("P12301_HUMAN")))
                .andExpect(jsonPath("$.representativeMember.proteinName", is("some protein name")))
                .andExpect(jsonPath("$.representativeMember.seed", is(true)))
                .andExpect(jsonPath("$.representativeMember.sequence.length", is(66)))
                .andExpect(jsonPath("$.members", contains("P12301", "P32101")))
                .andExpect(jsonPath("$.organisms.*.taxonId", contains(9600, 9607)))
                .andExpect(
                        jsonPath(
                                "$.organisms.*.scientificName",
                                contains("Homo sapiens", "Homo sapiens 1")));
    }

    @Test
    void invalidIdReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + "INVALID" + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "The 'id' value has invalid format. It should be a valid UniRef Cluster id")));
    }

    @Test
    void nonExistentIdReturnFoundRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + "UniRef50_P99999" + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void withFilterFieldsReturnSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "common_taxon,name");

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", is(ID)))
                .andExpect(jsonPath("$.name", is("Cluster: MoeK5 01")))
                .andExpect(jsonPath("$.commonTaxon.taxonId", is(9606)))
                .andExpect(jsonPath("$.commonTaxon.scientificName", is("Homo sapiens")))
                .andExpect(jsonPath("$.seedId").doesNotExist())
                .andExpect(jsonPath("$.representativeMember").doesNotExist())
                .andExpect(jsonPath("$.members").doesNotExist())
                .andExpect(jsonPath("$.organisms").doesNotExist());
    }

    @Test
    void withInvalidFilterFieldsReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "InvalidField,name");

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("Invalid fields parameter value 'InvalidField'")));
    }

    @Test
    void contentTypeFastaSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">UniRef50_P03901 some protein name n=2 Tax=Homo sapiens TaxID=9606 RepID=P12301_HUMAN")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "MVSWGRFICLVVVTMATLSLARPSFSLVEDDFSAGSADFAFWERDGDSDGFDSHSDJHET")))
                .andExpect(content().string(containsString("RHJREH")));
    }

    @Test
    void contentTypeListSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString(ID)));
    }

    @Test
    void contentTypeTsvSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Cluster ID\tCluster Name\tCommon taxon\tSize\tDate of creation")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "UniRef50_P03901\tCluster: MoeK5 01\tHomo sapiens\t2\t2019-08-27")));
    }

    @Test
    void contentTypeXlsSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.XLS_MEDIA_TYPE_VALUE));
    }

    @Test
    void contentTypeRdfSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(ID_LIGHT_PREFIX_PATH + ID + ID_LIGHT_SUFIX_PATH)
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.RDF_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "<?xml version='1.0' encoding='UTF-8'?>\n"
                                                        + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:uniparc=\"http://purl.uniprot.org/uniparc/\" xmlns:uniprot=\"http://purl.uniprot.org/uniprot/\" xmlns:uniref=\"http://purl.uniprot.org/uniref/\">\n"
                                                        + "<owl:Ontology rdf:about=\"\">\n"
                                                        + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                                                        + "</owl:Ontology>\n"
                                                        + "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n"
                                                        + "</rdf:RDF>")));
    }
}
