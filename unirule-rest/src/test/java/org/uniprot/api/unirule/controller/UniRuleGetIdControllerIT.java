package org.uniprot.api.unirule.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.unirule.UniRuleRestApplication;
import org.uniprot.api.unirule.repository.UniRuleQueryRepository;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.UniRuleId;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilder;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.core.unirule.impl.UniRuleIdBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.unirule.UniRuleDocumentConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            UniRuleRestApplication.class,
            ErrorHandlerConfig.class
        })
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
        UniRuleDocumentConverter converter = new UniRuleDocumentConverter();
        UniRuleDocument document = converter.convertToDocument(entry);
        getStoreManager().saveDocs(getStoreType(), document);
    }

    private UniRuleEntry create() {
        UniRuleId uniRuleId = new UniRuleIdBuilder(UNIRULE_ID).build();
        UniRuleEntry uniRule = UniRuleEntryBuilderTest.createObject();
        return UniRuleEntryBuilder.from(uniRule).uniRuleId(uniRuleId).build();
    }

    @Override
    protected String getIdRequestPath() {
        return "/unirule/";
    }

    static class UniRuleGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(UNIRULE_ID)
                    .resultMatcher(jsonPath("$.uniRuleId", is(UNIRULE_ID)))
                    .resultMatcher(jsonPath("$.information", notNullValue()))
                    .resultMatcher(jsonPath("$.ruleStatus", notNullValue()))
                    .resultMatcher(jsonPath("$.mainRule", notNullValue()))
                    .resultMatcher(jsonPath("$.otherRules", notNullValue()))
                    .resultMatcher(jsonPath("$.samFeatureSets", notNullValue()))
                    .resultMatcher(jsonPath("$.positionFeatureSets", notNullValue()))
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
                                            "The UniRule id value has invalid format. It should match the regular expression 'UR[0-9]{9}'")))
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
                    .resultMatcher(jsonPath("$.information", notNullValue()))
                    .resultMatcher(jsonPath("$.ruleStatus").doesNotExist())
                    .resultMatcher(jsonPath("$.mainRule").doesNotExist())
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
                                    .resultMatcher(jsonPath("$.ruleStatus", notNullValue()))
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
                                    .resultMatcher(
                                            jsonPath("$.proteinsAnnotatedCount", notNullValue()))
                                    .resultMatcher(jsonPath("$.createdBy", notNullValue()))
                                    .resultMatcher(jsonPath("$.modifiedBy", notNullValue()))
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
                                                            "The UniRule id value has invalid format. It should match the regular expression 'UR[0-9]{9}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}
