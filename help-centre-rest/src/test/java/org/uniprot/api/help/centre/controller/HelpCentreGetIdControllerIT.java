package org.uniprot.api.help.centre.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.help.centre.HelpCentreRestApplication;
import org.uniprot.api.help.centre.repository.HelpCentreQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 08/07/2021
 */
@ContextConfiguration(
        classes = {
            HelpCentreStoreTestConfig.class,
            HelpCentreRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(HelpCentreController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            HelpCentreGetIdControllerIT.HelpCentreGetIdParameterResolver.class,
            HelpCentreGetIdControllerIT.HelpCentreGetIdContentTypeParamResolver.class
        })
class HelpCentreGetIdControllerIT extends AbstractGetByIdControllerIT {
    private static final String ID = "help_id";
    public static final String TITLE = "Help Title";
    public static final String CONTENT_CLEAN = "clean content";
    public static final String CONTENT_ORIGINAL = "original content";
    public static final String CATEGORY = "categoryValue";

    @Autowired private HelpCentreQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.HELP;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.help;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {
        HelpDocument doc =
                HelpDocument.builder()
                        .id(ID)
                        .title(TITLE)
                        .content(CONTENT_CLEAN)
                        .contentOriginal(CONTENT_ORIGINAL)
                        .lastModified(new GregorianCalendar(2021, Calendar.JULY, 14).getTime())
                        .categories(List.of(CATEGORY))
                        .build();
        getStoreManager().saveDocs(getStoreType(), doc);
    }

    @Override
    protected String getIdRequestPath() {
        return "/help-centre/{id}";
    }

    static class HelpCentreGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder().id(ID).resultMatcher(jsonPath("$.id", is(ID))).build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("99999")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("The 'id' is invalid. It can not be a number.")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("does_not_exist")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(ID)
                    .fields("id")
                    .resultMatcher(jsonPath("$.id", is(ID)))
                    .resultMatcher(jsonPath("$.title").doesNotExist())
                    .resultMatcher(jsonPath("$.content").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(ID)
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class HelpCentreGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(ID)
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.id", is(ID)))
                                    .resultMatcher(jsonPath("$.title", is(TITLE)))
                                    .resultMatcher(jsonPath("$.content", is(CONTENT_ORIGINAL)))
                                    .resultMatcher(jsonPath("$.lastModified", is("2021-07-14")))
                                    .resultMatcher(jsonPath("$.categories", contains(CATEGORY)))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.MARKDOWN_MEDIA_TYPE)
                                    .resultMatcher(content().string(containsString(TITLE)))
                                    .resultMatcher(content().string(containsString(CATEGORY)))
                                    .resultMatcher(
                                            content().string(containsString(CONTENT_ORIGINAL)))
                                    .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("9999")
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(jsonPath("$.url", not(emptyOrNullString())))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The 'id' is invalid. It can not be a number.")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.MARKDOWN_MEDIA_TYPE)
                                    .resultMatcher(content().string(emptyString()))
                                    .build())
                    .build();
        }
    }
}
