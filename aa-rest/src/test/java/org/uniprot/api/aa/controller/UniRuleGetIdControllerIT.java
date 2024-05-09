package org.uniprot.api.aa.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.aa.AARestApplication;
import org.uniprot.api.aa.repository.UniRuleQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.unirule.Information;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.UniRuleId;
import org.uniprot.core.unirule.impl.InformationBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.core.unirule.impl.UniRuleIdBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.unirule.UniRuleDocumentConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@ContextConfiguration(classes = {AARestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRuleController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            UniRuleGetIdControllerIT.UniRuleGetIdParameterResolver.class,
            UniRuleGetIdControllerIT.UniRuleGetIdContentTypeParamResolver.class
        })
public class UniRuleGetIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String UNIRULE_ID = "UR000100241";
    private static final String OLD_RULE_ID = "MF_00807";

    @Autowired private UniRuleQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.UNIRULE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.unirule;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        UniRuleEntry entry = create();
        UniRuleDocumentConverter converter =
                new UniRuleDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
        UniRuleDocument document = converter.convertToDocument(entry);
        getStoreManager().saveDocs(getStoreType(), document);
    }

    private UniRuleEntry create() {
        UniRuleId uniRuleId = new UniRuleIdBuilder(UNIRULE_ID).build();
        UniRuleEntry uniRule = UniRuleEntryBuilderTest.createObject();
        // inject old rule id to search
        Information information =
                InformationBuilder.from(uniRule.getInformation()).oldRuleNum(OLD_RULE_ID).build();
        return UniRuleEntryBuilder.from(uniRule)
                .uniRuleId(uniRuleId)
                .information(information)
                .build();
    }

    @Override
    protected String getIdRequestPath() {
        return "/unirule/{uniruleid}";
    }

    @Test
    void testGetByOldRuleIdWithRedirect() throws Exception {
        // given
        saveEntry();

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), OLD_RULE_ID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        String redirectUri = "/unirule/" + UNIRULE_ID + "?from=" + OLD_RULE_ID;
        response.andDo(log())
                .andExpect(status().is(HttpStatus.SEE_OTHER.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string(HttpHeaders.LOCATION, redirectUri));

        // let's simulate a client (e.g., browser) redirect and go to the address directly
        response = getMockMvc().perform(get(redirectUri));
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(
                                                HttpHeaders.CONTENT_TYPE,
                                                MediaType.APPLICATION_JSON_VALUE));
        GetIdParameter idParameter = new UniRuleGetIdParameterResolver().validIdParameter();
        for (ResultMatcher resultMatcher : idParameter.getResultMatchers()) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @ParameterizedTest
    @MethodSource("provideOldRuleNumber")
    void testOldRuleNumberValidation(String ruleId) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), ruleId).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then no validation error
        response.andDo(log())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    private static Stream<Arguments> provideOldRuleNumber() {
        return List.of("MF_00855", "PIRSR620777-50", "PIRNR029950", "RU366062", "PRU01042").stream()
                .map(Arguments::of);
    }

    static class UniRuleGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIRULE_ID)
                    .resultMatcher(jsonPath("$.uniRuleId", is(UNIRULE_ID)))
                    .resultMatcher(jsonPath("$.information", notNullValue()))
                    .resultMatcher(jsonPath("$.mainRule", notNullValue()))
                    .resultMatcher(jsonPath("$.otherRules", notNullValue()))
                    .resultMatcher(jsonPath("$.samFeatureSets", notNullValue()))
                    .resultMatcher(jsonPath("$.positionFeatureSets", notNullValue()))
                    .resultMatcher(jsonPath("$.statistics", notNullValue()))
                    .resultMatcher(jsonPath("$.statistics.reviewedProteinCount", notNullValue()))
                    .resultMatcher(jsonPath("$.statistics.unreviewedProteinCount", notNullValue()))
                    .resultMatcher(jsonPath("$.createdDate", notNullValue()))
                    .resultMatcher(jsonPath("$.modifiedDate", notNullValue()))
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
                                            "The UniRule id value has invalid format. It should match the regular expression 'UR[0-9]{9}|MF_[0-9]{5}|PIRSR[0-9]+(-[0-9]+)?|PIRNR[0-9]+|RU[0-9]{6}|PRU[0-9]{5}'")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("UR123456789")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(UNIRULE_ID)
                    .fields("template_entries")
                    .resultMatcher(jsonPath("$.uniRuleId", is(UNIRULE_ID)))
                    .resultMatcher(jsonPath("$.information.oldRuleNum", is(OLD_RULE_ID)))
                    .resultMatcher(jsonPath("$.mainRule").doesNotExist())
                    .resultMatcher(jsonPath("$.otherRules").doesNotExist())
                    .resultMatcher(jsonPath("$.samFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.positionFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.statistics").doesNotExist())
                    .resultMatcher(jsonPath("$.createdDate").doesNotExist())
                    .resultMatcher(jsonPath("$.modifiedDate").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(UNIRULE_ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class UniRuleGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(UNIRULE_ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.uniRuleId", is(UNIRULE_ID)))
                                    .resultMatcher(jsonPath("$.information", notNullValue()))
                                    .resultMatcher(jsonPath("$.mainRule", notNullValue()))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.otherRules",
                                                    Matchers.hasSize(greaterThan(0))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.samFeatureSets",
                                                    Matchers.hasSize(greaterThan(0))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.positionFeatureSets",
                                                    Matchers.hasSize(greaterThan(0))))
                                    .resultMatcher(jsonPath("$.statistics", notNullValue()))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.statistics.reviewedProteinCount",
                                                    notNullValue()))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.statistics.unreviewedProteinCount",
                                                    notNullValue()))
                                    .resultMatcher(jsonPath("$.createdDate", notNullValue()))
                                    .resultMatcher(jsonPath("$.modifiedDate", notNullValue()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(UNIRULE_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UniRule ID\tTemplate Entries")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "UR000100241\taccession-")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
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
                                                            "The UniRule id value has invalid format. It should match the regular expression 'UR[0-9]{9}|MF_[0-9]{5}|PIRSR[0-9]+(-[0-9]+)?|PIRNR[0-9]+|RU[0-9]{6}|PRU[0-9]{5}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe UniRule id value has invalid format. It should match the regular expression 'UR[0-9]{9}|MF_[0-9]{5}|PIRSR[0-9]+(-[0-9]+)?|PIRNR[0-9]+|RU[0-9]{6}|PRU[0-9]{5}'")))
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
                                                            is(
                                                                    "Error messages\nThe UniRule id value has invalid format. It should match the regular expression 'UR[0-9]{9}|MF_[0-9]{5}|PIRSR[0-9]+(-[0-9]+)?|PIRNR[0-9]+|RU[0-9]{6}|PRU[0-9]{5}'")))
                                    .build())
                    .build();
        }
    }
}
