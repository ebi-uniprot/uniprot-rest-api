package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetByIdParameterResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;

@ContextConfiguration(classes = {UniParcRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcEntryLightController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcLightGetIdControllerIT.UniParcLightGetByIdParameterResolver.class,
            UniParcLightGetIdControllerIT.UniParcLightGetIdContentTypeParamResolver.class
        })
class UniParcLightGetIdControllerIT extends AbstractGetSingleUniParcByIdTest {

    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    protected String getIdRequestPath() {
        return "/uniparc/{upi}/light";
    }

    @Override
    protected String getIdPathValue() {
        return UNIPARC_ID;
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    static class UniParcLightGetByIdParameterResolver extends AbstractGetByIdParameterResolver {

        @Override
        protected GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.oldestCrossRefCreated").exists())
                    .resultMatcher(jsonPath("$.mostRecentCrossRefUpdated").exists())
                    .resultMatcher(jsonPath("$.crossReferenceCount", is(25)))
                    .resultMatcher(
                            jsonPath(
                                    "$.commonTaxons[*].topLevel",
                                    contains("cellular organisms", "other entries")))
                    .resultMatcher(
                            jsonPath(
                                    "$.commonTaxons[*].commonTaxon",
                                    contains("Bacteria", "plasmids")))
                    .resultMatcher(jsonPath("$.uniProtKBAccessions", contains("P12301")))
                    .resultMatcher(jsonPath("$.sequence.value", is("MLMPKRTKYRA")))
                    .resultMatcher(jsonPath("$.sequenceFeatures.size()", is(13)))
                    .build();
        }

        @Override
        protected GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.size()", is(1)))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                    .build();
        }

        @Override
        protected GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UPI0000083A99")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        protected GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .fields("upi,accession")
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.id").exists())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.database").exists())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.active").exists())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.version").exists())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.chain").exists())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.organism").doesNotExist())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences.*.proteomeId").doesNotExist())
                    .resultMatcher(jsonPath("$.sequence").doesNotExist())
                    .resultMatcher(jsonPath("$.sequenceFeatures").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.oldestCrossRefCreated").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.mostRecentCrossRefUpdated").doesNotExist())
                    .build();
        }

        @Override
        protected GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class UniParcLightGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        protected GetIdContentTypeParam idSuccessContentTypesParam() {
            return null;
        }

        @Override
        protected GetIdContentTypeParam idBadRequestContentTypesParam() {
            return null;
        }
    }

    //    @BeforeAll
    //    void initDataStore() {
    //        storeManager.addSolrClient(
    //                DataStoreManager.StoreType.UNIPARC_LIGHT, SolrCollection.uniparc);
    //        storeManager.addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, storeClient);
    //        storeManager.addStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE,
    // xRefStoreClient);
    //
    //        ReflectionTestUtils.setField(
    //                repository,
    //                "solrClient",
    //                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC_LIGHT));
    //        saveEntry();
    //    }

    //    @AfterAll
    //    void cleanStoreClient() {
    //        storeClient.truncate();
    //        xRefStoreClient.truncate();
    //    }

    //    protected void saveEntry() {
    //        UniParcEntry entry = createUniParcEntry(1, UPI_PREF);
    //
    //        UniParcDocument.UniParcDocumentBuilder docBuilder =
    //                UniParcITUtils.getUniParcDocument(entry);
    //        storeManager.saveDocs(DataStoreManager.StoreType.UNIPARC_LIGHT, docBuilder.build());
    //
    //        UniParcEntryLight entryLight = convertToUniParcEntryLight(entry);
    //        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, entryLight);
    //        List<UniParcCrossReferencePair> xrefPairs =
    //                UniParcCrossReferenceMocker.createCrossReferencePairsFromXRefs(
    //                        entryLight.getUniParcId(),
    //                        xrefGroupSize,
    //                        entry.getUniParcCrossReferences());
    //        for (UniParcCrossReferencePair xrefPair : xrefPairs) {
    //            xRefStoreClient.saveEntry(xrefPair);
    //        }
    //    }

    /* @Test


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
    }*/
}
