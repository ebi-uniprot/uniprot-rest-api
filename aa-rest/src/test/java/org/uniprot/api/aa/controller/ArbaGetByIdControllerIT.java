package org.uniprot.api.aa.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.aa.AARestApplication;
import org.uniprot.api.aa.repository.ArbaQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.UniRuleId;
import org.uniprot.core.unirule.impl.InformationBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.core.unirule.impl.UniRuleIdBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.arba.ArbaDocumentConverter;
import org.uniprot.store.search.SolrCollection;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@ContextConfiguration(
        classes = {DataStoreTestConfig.class, AARestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ArbaController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            ArbaGetByIdControllerIT.ArbaGetByIdParameterResolver.class,
            ArbaGetByIdControllerIT.ArbaGetByIdContentTypeParamResolver.class
        })
public class ArbaGetByIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String PATH = "/arba/{arbaId}";
    private static final String ARBA_ID = "ARBA00000103";

    @Autowired private ArbaQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.ARBA;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.arba;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return this.repository;
    }

    @Override
    protected void saveEntry() {
        UniRuleEntry uniRuleEntry = create();
        var converter = new ArbaDocumentConverter();
        var arbaDocument = converter.convertToDocument(uniRuleEntry);
        getStoreManager().saveDocs(getStoreType(), arbaDocument);
    }

    private UniRuleEntry create() {
        UniRuleId uniRuleId = new UniRuleIdBuilder(ARBA_ID).build();
        UniRuleEntry uniRule = UniRuleEntryBuilderTest.createObject();
        // remove unnecessary fields for ARBA
        var builder = UniRuleEntryBuilder.from(uniRule);
        InformationBuilder infoBuilder = new InformationBuilder("0");
        builder.information(infoBuilder.build());
        return builder.uniRuleId(uniRuleId)
                .otherRulesSet(null)
                .positionFeatureSetsSet(null)
                .samFeatureSetsSet(null)
                .build();
    }

    @Override
    protected String getIdRequestPath() {
        return PATH;
    }

    static class ArbaGetByIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(ARBA_ID)
                    .resultMatcher(jsonPath("$.uniRuleId", is(ARBA_ID)))
                    .resultMatcher(jsonPath("$.information", notNullValue()))
                    .resultMatcher(jsonPath("$.ruleStatus", notNullValue()))
                    .resultMatcher(jsonPath("$.mainRule", notNullValue()))
                    .resultMatcher(jsonPath("$.otherRules").doesNotExist())
                    .resultMatcher(jsonPath("$.samFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.positionFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.proteinsAnnotatedCount", notNullValue()))
                    .resultMatcher(jsonPath("$.createdBy", notNullValue()))
                    .resultMatcher(jsonPath("$.modifiedBy", notNullValue()))
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
                                            "The ARBA id value has invalid format. It should match the regular expression 'ARBA[0-9]{8}'")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("ARBA99999999")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ARBA_ID)
                    .fields("annotation_covered")
                    .resultMatcher(jsonPath("$.uniRuleId", is(ARBA_ID)))
                    .resultMatcher(jsonPath("$.information").doesNotExist())
                    .resultMatcher(jsonPath("$.ruleStatus").doesNotExist())
                    .resultMatcher(jsonPath("$.mainRule").exists())
                    .resultMatcher(jsonPath("$.otherRules").doesNotExist())
                    .resultMatcher(jsonPath("$.samFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.positionFeatureSets").doesNotExist())
                    .resultMatcher(jsonPath("$.proteinsAnnotatedCount").doesNotExist())
                    .resultMatcher(jsonPath("$.createdBy").doesNotExist())
                    .resultMatcher(jsonPath("$.modifiedBy").doesNotExist())
                    .resultMatcher(jsonPath("$.createdDate").doesNotExist())
                    .resultMatcher(jsonPath("$.modifiedDate").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ARBA_ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class ArbaGetByIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        protected GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ARBA_ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(ARBA_ID)))
                                    .build())
                    .build();
        }

        @Override
        protected GetIdContentTypeParam idBadRequestContentTypesParam() {
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
                                                            "The ARBA id value has invalid format. It should match the regular expression 'ARBA[0-9]{8}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}