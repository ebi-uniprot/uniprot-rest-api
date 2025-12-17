package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.*;
import static org.uniprot.api.rest.output.converter.ConverterConstants.*;
import static org.uniprot.api.uniparc.controller.UniParcGetByAccessionControllerIT.SOURCES;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetByIdParameterResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.api.uniparc.common.repository.UniParcDataStoreTestConfig;
import org.uniprot.api.uniparc.common.repository.UniParcStreamConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
@ContextConfiguration(
        classes = {
            UniParcStreamConfig.class,
            UniParcDataStoreTestConfig.class,
            UniParcRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniParcGetIdControllerIT.UniParcGetByIdParameterResolver.class,
            UniParcGetIdControllerIT.UniParcGetIdContentTypeParamResolver.class
        })
public class UniParcGetIdControllerIT extends AbstractGetSingleUniParcByIdTest {
    @MockBean(name = "uniParcRdfRestTemplate")
    private RestTemplate restTemplate;

    @Override
    protected String getIdPathValue() {
        return UNIPARC_ID;
    }

    @Override
    protected String getIdRequestPath() {
        return "/uniparc/{upi}";
    }

    @BeforeEach
    void setUp() {
        ControllerITUtils.mockRestTemplateResponsesForRDFFormats(restTemplate, "uniparc");
    }

    @Test
    void testGetByUniParcIdWithFieldsThatDoesNotHaveValuesInEntry() throws Exception {
        // given
        UniParcEntryConverter converter = new UniParcEntryConverter();
        UniParcEntry simpleEntry = UniParcEntryMocker.createUniParcEntry(2, "UPI0000083D");
        simpleEntry = UniParcEntryBuilder.from(simpleEntry).sequenceFeaturesSet(List.of()).build();
        String uniParcId = simpleEntry.getUniParcId().getValue();
        Entry simpleXmlEntry = converter.toXml(simpleEntry);
        getStoreManager().saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, simpleXmlEntry);
        // put uniparc light and cross references in voldemort
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.convertToUniParcEntryLight(simpleEntry);
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcEntryLight);
        UniParcCrossReferencePair pair =
                new UniParcCrossReferencePair(
                        uniParcId + "_0", simpleEntry.getUniParcCrossReferences());
        getStoreManager().saveToStore(DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, pair);

        // when
        ResultActions response =
                getMockMvc()
                        .perform(
                                MockMvcRequestBuilders.get(getIdRequestPath(), uniParcId)
                                        .param("fields", "CDD"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo(uniParcId)))
                .andExpect(jsonPath("$.sequenceFeatures").doesNotExist());
    }

    static class UniParcGetByIdParameterResolver extends AbstractGetByIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIPARC_ID)
                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                    .resultMatcher(jsonPath("$.oldestCrossRefCreated").exists())
                    .resultMatcher(jsonPath("$.mostRecentCrossRefUpdated").exists())
                    .resultMatcher(jsonPath("$.uniParcCrossReferences", is(not(empty()))))
                    .resultMatcher(jsonPath("$.uniParcCrossReferences", hasSize(25)))
                    .resultMatcher(
                            jsonPath(
                                    "$.uniParcCrossReferences[*].proteomeIdComponents",
                                    everyItem(not(empty()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.uniParcCrossReferences[*].proteomeIdComponents[*].proteomeId",
                                    not(empty())))
                    .resultMatcher(
                            jsonPath(
                                    "$.uniParcCrossReferences[*].proteomeIdComponents[*].component",
                                    not(empty())))
                    .resultMatcher(
                            jsonPath(
                                    "$.uniParcCrossReferences[*].proteomeIdComponents[*].proteomeId",
                                    notNullValue()))
                    .resultMatcher(
                            jsonPath(
                                    "$.uniParcCrossReferences[*].proteomeIdComponents[*].component",
                                    notNullValue()))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UPI0000083A99")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
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
                    .resultMatcher(
                            jsonPath(
                                    "$.uniParcCrossReferences[*].properties[*].key",
                                    not("sources")))
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
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

    static class UniParcGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(UNIPARC_ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.uniParcId", is(UNIPARC_ID)))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.sequenceFeatures[*].properties[*].key",
                                                    not("sources")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(UNIPARC_ID)))
                                    .resultMatcher(content().string(not(containsString(SOURCES))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            startsWith(
                                                                    XML_DECLARATION
                                                                            + UNIPARC_XML_SCHEMA)))
                                    .resultMatcher(content().string(containsString(UNIPARC_ID)))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            not(
                                                                    containsString(
                                                                            ("<property type=\"sources\"")))))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            endsWith(
                                                                    COPYRIGHT_TAG
                                                                            + UNIPARC_XML_CLOSE_TAG)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(containsString(SOURCES))))
                                    .resultMatcher(content().string(equalTo(SAMPLE_RDF)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(containsString(SOURCES))))
                                    .resultMatcher(content().string(equalTo(SAMPLE_TTL)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(containsString(SOURCES))))
                                    .resultMatcher(content().string(equalTo(SAMPLE_N_TRIPLES)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(containsString(SOURCES))))
                                    .resultMatcher(content().string(containsString(UNIPARC_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(not(containsString(SOURCES))))
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TURTLE_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.N_TRIPLES_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'upi' value has invalid format. It should be a valid UniParc UPI")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'upi' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            "Error messages\nThe 'upi' value has invalid format. It should be a valid UniParc UPI"))
                                    .build())
                    .build();
        }
    }
}
