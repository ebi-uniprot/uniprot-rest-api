package org.uniprot.api.support.data.keyword.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;

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
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.keyword.repository.KeywordRepository;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.cv.keyword.KeywordId;
import org.uniprot.core.cv.keyword.impl.KeywordEntryBuilder;
import org.uniprot.core.cv.keyword.impl.KeywordIdBuilder;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(KeywordController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            KeywordGetIdControllerIT.KeywordGetIdParameterResolver.class,
            KeywordGetIdControllerIT.KeywordGetIdContentTypeParamResolver.class
        })
public class KeywordGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final String KEYWORD_ACCESSION = "KW-0005";

    @Autowired private KeywordRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.KEYWORD;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.keyword;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        KeywordId keyword = new KeywordIdBuilder().name("my keyword").id(KEYWORD_ACCESSION).build();
        KeywordId category = new KeywordIdBuilder().name("Ligand").id("KW-9993").build();

        KeywordEntry keywordEntry =
                new KeywordEntryBuilder()
                        .definition("Definition value")
                        .keyword(keyword)
                        .category(category)
                        .build();
        KeywordDocument document =
                KeywordDocument.builder()
                        .id(KEYWORD_ACCESSION)
                        .keywordObj(getKeywordBinary(keywordEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.KEYWORD, document);
    }

    @Override
    protected String getIdRequestPath() {
        return "/keyword/";
    }

    private ByteBuffer getKeywordBinary(KeywordEntry entry) {
        try {
            return ByteBuffer.wrap(
                    KeywordJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse KeywordEntry to binary json: ", e);
        }
    }

    static class KeywordGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(KEYWORD_ACCESSION)
                    .resultMatcher(jsonPath("$.keyword.id", is(KEYWORD_ACCESSION)))
                    .resultMatcher(jsonPath("$.keyword.name", is("my keyword")))
                    .resultMatcher(jsonPath("$.definition", is("Definition value")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The keyword id value has invalid format. It should match the regular expression 'KW-[0-9]{4}'")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("KW-0000")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(KEYWORD_ACCESSION)
                    .fields("id,name,category")
                    .resultMatcher(jsonPath("$.keyword.id", is(KEYWORD_ACCESSION)))
                    .resultMatcher(jsonPath("$.keyword.name", is("my keyword")))
                    .resultMatcher(jsonPath("$.category").exists())
                    .resultMatcher(jsonPath("$.definition").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(KEYWORD_ACCESSION)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class KeywordGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(KEYWORD_ACCESSION)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.keyword.id", is(KEYWORD_ACCESSION)))
                                    .resultMatcher(jsonPath("$.keyword.name", is("my keyword")))
                                    .resultMatcher(jsonPath("$.definition", is("Definition value")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().string(containsString(KEYWORD_ACCESSION)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "Keyword ID\tName\tDescription\tCategory")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "KW-0005\tmy keyword\tDefinition value\tLigand")))
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
                                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The keyword id value has invalid format. It should match the regular expression 'KW-[0-9]{4}'")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(content().string(isEmptyString()))
                                    .build())
                    .build();
        }
    }
}
