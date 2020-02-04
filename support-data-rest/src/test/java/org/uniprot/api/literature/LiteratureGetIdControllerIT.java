package org.uniprot.api.literature;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.DataStoreTestConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.literature.repository.LiteratureRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.support_data.SupportDataApplication;
import org.uniprot.core.DBCrossReference;
import org.uniprot.core.builder.DBCrossReferenceBuilder;
import org.uniprot.core.citation.CitationXrefType;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.builder.LiteratureBuilder;
import org.uniprot.core.citation.impl.AuthorImpl;
import org.uniprot.core.citation.impl.PublicationDateImpl;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.literature.builder.LiteratureStoreEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.field.LiteratureField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            LiteratureGetIdControllerIT.LiteratureGetIdParameterResolver.class,
            LiteratureGetIdControllerIT.LiteratureGetIdContentTypeParamResolver.class
        })
class LiteratureGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final long PUBMED_ID = 100L;

    @Autowired private LiteratureRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.LITERATURE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.literature;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected void saveEntry() {

        DBCrossReference<CitationXrefType> pubmed =
                new DBCrossReferenceBuilder<CitationXrefType>()
                        .databaseType(CitationXrefType.PUBMED)
                        .id(String.valueOf(PUBMED_ID))
                        .build();

        Literature literature =
                new LiteratureBuilder()
                        .addCitationXrefs(pubmed)
                        .title("The Title")
                        .authorsAdd(new AuthorImpl("The Author"))
                        .literatureAbstract("literature abstract")
                        .publicationDate(new PublicationDateImpl("2019"))
                        .firstPage("10")
                        .build();

        LiteratureEntry literatureEntry = new LiteratureEntryBuilder().citation(literature).build();

        LiteratureStoreEntry storeEntry =
                new LiteratureStoreEntryBuilder().literatureEntry(literatureEntry).build();

        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(String.valueOf(PUBMED_ID))
                        .literatureObj(getLiteratureBinary(storeEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    @Override
    protected String getIdRequestPath() {
        return "/literature/";
    }

    private ByteBuffer getLiteratureBinary(LiteratureStoreEntry entry) {
        try {
            return ByteBuffer.wrap(
                    LiteratureJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }

    static class LiteratureGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .resultMatcher(jsonPath("$.citation.citationXrefs[0].id", is("100")))
                    .resultMatcher(jsonPath("$.citation.authors", contains("The Author")))
                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
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
                                    contains("The PubMed id value should be a number")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("999")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .fields("id,title")
                    .resultMatcher(jsonPath("$.citation.citationXrefs[0].id", is("100")))
                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                    .resultMatcher(jsonPath("$.citation.authors").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }

        @Override
        public GetIdParameter withValidResponseFieldsOrderParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .resultMatcher(
                            result -> {
                                String contentAsString = result.getResponse().getContentAsString();
                                try {
                                    Map<String, Object> responseMap =
                                            new ObjectMapper()
                                                    .readValue(
                                                            contentAsString, LinkedHashMap.class);
                                    List<String> actualList = new ArrayList<>(responseMap.keySet());
                                    List<String> expectedList = getFieldsInOrder();
                                    Assertions.assertEquals(expectedList.size(), actualList.size());
                                    Assertions.assertEquals(expectedList, actualList);
                                } catch (IOException e) {
                                    Assertions.fail(e.getMessage());
                                }
                            })
                    .build();
        }

        private List<String> getFieldsInOrder() {
            List<String> fields = new LinkedList<>();
            fields.add(LiteratureField.ResultFields.id.getJavaFieldName());
            fields.add(LiteratureField.ResultFields.title.getJavaFieldName());
            fields.add(LiteratureField.ResultFields.author.getJavaFieldName());
            fields.add(LiteratureField.ResultFields.publication.getJavaFieldName());
            fields.add(LiteratureField.ResultFields.lit_abstract.getJavaFieldName());
            fields.add(LiteratureField.ResultFields.first_page.getJavaFieldName());
            fields.add(LiteratureField.ResultFields.completeAuthorList.getJavaFieldName());
            return fields;
        }
    }

    static class LiteratureGetIdContentTypeParamResolver
            extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .resultMatcher(
                                            jsonPath("$.citation.citationXrefs[0].id", is("100")))
                                    .resultMatcher(
                                            jsonPath("$.citation.authors", contains("The Author")))
                                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    String.valueOf(PUBMED_ID))))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "PubMed ID\tTitle\tReference\tAbstract/Summary")))
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "100\tThe Title\t10(2019)\tliterature abstract")))
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
                                                            "The PubMed id value should be a number")))
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
