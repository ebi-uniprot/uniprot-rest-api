package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.*;

import java.util.List;

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
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.util.Utils;
import org.uniprot.cv.taxonomy.TaxonomicNode;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.util.TaxonomyRepoUtil;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.document.uniparc.UniParcDocumentConverter;

@ContextConfiguration(classes = {UniParcRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcEntryLightController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
class UniParcLightGetIdControllerIT {

    private static final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();
    private static final String UPI_PREF = "UPI0000083D";
    public static final String UNIPARC_ID = "UPI0000083D01";

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    @RegisterExtension private static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private UniParcLightStoreClient storeClient;

    @Autowired private UniParcCrossReferenceStoreClient xRefStoreClient;

    @Autowired private MockMvc mockMvc;

    @Autowired private UniParcQueryRepository repository;

    @BeforeAll
    void initDataStore() {
        storeManager.addSolrClient(
                DataStoreManager.StoreType.UNIPARC_LIGHT, SolrCollection.uniparc);
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, storeClient);
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, xRefStoreClient);

        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC_LIGHT));
        saveEntry();
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    @AfterAll
    void cleanStoreClient() {
        storeClient.truncate();
        xRefStoreClient.truncate();
    }

    protected void saveEntry() {
        UniParcEntry entry = createUniParcEntry(1, UPI_PREF);

        UniParcDocumentConverter converter = new UniParcDocumentConverter();
        UniParcDocument doc = converter.convert(entry);
        UniParcDocument.UniParcDocumentBuilder builder = doc.toBuilder();
        for (UniParcCrossReference xref : entry.getUniParcCrossReferences()) {
            if (Utils.notNull(xref.getOrganism())) {
                List<TaxonomicNode> nodes =
                        TaxonomyRepoUtil.getTaxonomyLineage(
                                taxonomyRepo, (int) xref.getOrganism().getTaxonId());
                builder.organismId((int) xref.getOrganism().getTaxonId());
                nodes.forEach(
                        node -> {
                            builder.taxLineageId(node.id());
                            List<String> names = TaxonomyRepoUtil.extractTaxonFromNode(node);
                            names.forEach(builder::organismTaxon);
                        });
            }
        }
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPARC_LIGHT, doc);

        UniParcEntryLight entryLight = convertToUniParcEntryLight(entry);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, entryLight);
        for (UniParcCrossReference xref : entry.getUniParcCrossReferences()) {
            String key = ""; // FIXME
            xRefStoreClient.saveEntry(
                    key,
                    new UniParcCrossReferencePair(
                            entryLight.getUniParcId(),
                            List.of(xref))); // TODO create the page logic here
        }
    }

    protected String getIdRequestPath() {
        return "/uniparc/{upi}/light";
    }

    @Test
    void validIdReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), UNIPARC_ID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                .andExpect(
                        jsonPath(
                                "$.crossReferenceCount",
                                is(3)))
                .andExpect(
                        jsonPath(
                                "$.commonTaxons[*].topLevel",
                                contains("cellular organisms", "other entries")))
                .andExpect(
                        jsonPath("$.commonTaxons[*].commonTaxon", contains("Bacteria", "plasmids")))
                .andExpect(jsonPath("$.uniProtKBAccessions", contains("P10001", "P12301")))
                .andExpect(jsonPath("$.sequence.value", is("MLMPKRTKYRA")))
                .andExpect(jsonPath("$.sequenceFeatures.size()", is(12)));
    }

    @Test
    void invalidIdReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), "INVALID").header(ACCEPT, MediaType.APPLICATION_JSON);

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
                                        "The 'upi' value has invalid format. It should be a valid UniParc UPI")));
    }

    @Test
    void nonExistentIdReturnFoundRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), UPI_PREF + "10").header(ACCEPT, MediaType.APPLICATION_JSON);

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
                get(getIdRequestPath(), UNIPARC_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "upi,gene");

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                .andExpect(jsonPath("$.geneNames", contains("geneName01")))
                .andExpect(jsonPath("$.commonTaxons").doesNotExist())
                .andExpect(jsonPath("$.uniProtKBAccessions").doesNotExist())
                .andExpect(jsonPath("$.sequenceFeatures").doesNotExist());
    }

    @Test
    void withInvalidFilterFieldsReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), UNIPARC_ID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "InvalidField,upi");

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
                get(getIdRequestPath(), UNIPARC_ID)
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString(">UPI0000083D01 status=active")))
                .andExpect(content().string(containsString("MLMPKRTKYRA")));
    }

    @Test
    void contentTypeTsvSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), UNIPARC_ID)
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
                                                "Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "UPI0000083D01\tName 7787; Name 9606\tP10001; P12301\t2017-02-12\t2017-04-23\t11")));
    }

    @Test
    void contentTypeXlsSuccessRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), UNIPARC_ID)
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
                get(getIdRequestPath(), UNIPARC_ID)
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
                                                        + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:embl-cds=\"http://purl.uniprot.org/embl-cds/\" xmlns:ensembl=\"http://rdf.ebi.ac.uk/resource/ensembl/\" xmlns:faldo=\"http://biohackathon.org/resource/faldo#\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" xmlns:ssmRegion=\"http://purl.uniprot.org/signatureSequenceMatch/\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:uniparc=\"http://purl.uniprot.org/uniparc/\" xmlns:uniprot=\"http://purl.uniprot.org/uniprot/\">\n"
                                                        + "<owl:Ontology rdf:about=\"\">\n"
                                                        + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                                                        + "</owl:Ontology>\n"
                                                        + "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n"
                                                        + "</rdf:RDF>")));
    }
}
