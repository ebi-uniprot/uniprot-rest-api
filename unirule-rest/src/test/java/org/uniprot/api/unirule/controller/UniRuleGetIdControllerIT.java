package org.uniprot.api.unirule.controller;

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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
        return SolrCollection.genecentric;
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
                    .resultMatcher(jsonPath("$.canonicalProtein.id", is(UNIRULE_ID)))
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
                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("P21910")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(UNIRULE_ID)
                    .fields("accession")
                    .resultMatcher(jsonPath("$.canonicalProtein.id", is(UNIRULE_ID)))
                    .resultMatcher(jsonPath("$.canonicalProtein.proteinName").doesNotExist())
                    .resultMatcher(jsonPath("$.canonicalProtein.geneName").doesNotExist())
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
                                    .resultMatcher(jsonPath("$.canonicalProtein.id", is(UNIRULE_ID)))
                                    .resultMatcher(jsonPath("$.proteomeId", is("UP000000554")))
                                    .resultMatcher(jsonPath("$.relatedProteins.size()", is(2)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content().string(containsString("<id>P21312</id>")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "<proteomeId>UP000000554</proteomeId>")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(UNIRULE_ID)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">sp|P21312|uniprotkb_id protein name OS=Human OX=9606 GN=some gene PE=1 SV=2")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    ">tr|P31912|uniprotkb_id protein name OS=Human OX=9606 GN=some gene3 PE=5 SV=2")))
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
                                                            "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_XML)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The 'accession' value has invalid format. It should be a valid UniProtKB accession")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}
