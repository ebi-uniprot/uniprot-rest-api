package org.uniprot.api.support.data.literature.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractGetByIdWithTypeExtensionControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.core.CrossReference;
import org.uniprot.core.citation.CitationDatabase;
import org.uniprot.core.citation.Literature;
import org.uniprot.core.citation.Submission;
import org.uniprot.core.citation.SubmissionDatabase;
import org.uniprot.core.citation.impl.AuthorBuilder;
import org.uniprot.core.citation.impl.LiteratureBuilder;
import org.uniprot.core.citation.impl.PublicationDateBuilder;
import org.uniprot.core.citation.impl.SubmissionBuilder;
import org.uniprot.core.impl.CrossReferenceBuilder;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.impl.LiteratureEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
            LiteratureGetIdControllerIT.LiteratureGetIdParameterResolver.class,
            LiteratureGetIdControllerIT.LiteratureGetIdContentTypeParamResolver.class
        })
class LiteratureGetIdControllerIT extends AbstractGetByIdWithTypeExtensionControllerIT {
    @Autowired
    @Qualifier("literatureRDFRestTemplate")
    private RestTemplate restTemplate;

    private static final long PUBMED_ID = 100L;
    private static final String SUBMISSION_ID = "CI-6LG40CJ34FGTT";

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

        CrossReference<CitationDatabase> pubmed =
                new CrossReferenceBuilder<CitationDatabase>()
                        .database(CitationDatabase.PUBMED)
                        .id(String.valueOf(PUBMED_ID))
                        .build();

        Literature literature =
                new LiteratureBuilder()
                        .citationCrossReferencesAdd(pubmed)
                        .title("The Title")
                        .authorsAdd(new AuthorBuilder("The Author").build())
                        .literatureAbstract("literature abstract")
                        .publicationDate(new PublicationDateBuilder("2019").build())
                        .firstPage("10")
                        .build();

        LiteratureEntry literatureEntry = new LiteratureEntryBuilder().citation(literature).build();

        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(String.valueOf(PUBMED_ID))
                        .literatureObj(getLiteratureBinary(literatureEntry))
                        .build();

        this.getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);

        Submission submission =
                new SubmissionBuilder()
                        .title("The Submission Title")
                        .authorsAdd(new AuthorBuilder("The Submission Author").build())
                        .publicationDate(new PublicationDateBuilder("2021").build())
                        .submittedToDatabase(SubmissionDatabase.PDB)
                        .build();

        literatureEntry = new LiteratureEntryBuilder().citation(submission).build();

        document =
                LiteratureDocument.builder()
                        .id(SUBMISSION_ID)
                        .literatureObj(getLiteratureBinary(literatureEntry))
                        .build();
        this.getStoreManager().saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    @Test
    void validSubmissionId() throws Exception {
        // given
        saveEntry();

        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPath(), SUBMISSION_ID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.citation.id", is(SUBMISSION_ID)))
                .andExpect(jsonPath("$.citation.authors", contains("The Submission Author")))
                .andExpect(jsonPath("$.citation.title", is("The Submission Title")));
    }

    @Override
    protected String getIdRequestPath() {
        return "/citations/{citationId}";
    }

    private byte[] getLiteratureBinary(LiteratureEntry entry) {
        try {
            return LiteratureJsonConfig.getInstance()
                    .getFullObjectMapper()
                    .writeValueAsBytes(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }

    @Override
    protected RestTemplate getRestTemple() {
        return restTemplate;
    }

    @Override
    protected String getSearchAccession() {
        return String.valueOf(PUBMED_ID);
    }

    @Override
    protected String getRDFProlog() {
        return RDFPrologs.LITERATURE_PROLOG;
    }

    @Override
    protected String getIdRequestPathWithoutPathVariable() {
        return "/citations/";
    }

    @Test
    void getBySubmissionIdWithRDFExtensionFailure() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPathWithoutPathVariable() + SUBMISSION_ID + ".rdf")
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        ResultActions response = getMockMvc().perform(requestBuilder);
        // then
        response.andDo(log()).andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    static class LiteratureGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .resultMatcher(jsonPath("$.citation.id", is("100")))
                    .resultMatcher(jsonPath("$.citation.citationCrossReferences[0].id", is("100")))
                    .resultMatcher(jsonPath("$.citation.authors", contains("The Author")))
                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder()
                    .id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains(
                                            "The citation id has invalid format. It should be a PubMedId (number) or with CI-\\w{1,13} or IND[0-9]+")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder()
                    .id("999")
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .fields("id,title")
                    .resultMatcher(jsonPath("$.citation.id", is("100")))
                    .resultMatcher(jsonPath("$.citation.title", is("The Title")))
                    .resultMatcher(jsonPath("$.citation.authors").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                    .resultMatcher(
                            jsonPath(
                                    "$.messages.*",
                                    contains("Invalid fields parameter value 'invalid'")))
                    .build();
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
                                            jsonPath(
                                                    "$.citation.citationCrossReferences[0].id",
                                                    is("100")))
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
                                                                    "Citation Id\tTitle\tReference\tAbstract/Summary")))
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
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.RDF_MEDIA_TYPE))
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
                                    .resultMatcher(jsonPath("$.url", not(is(emptyOrNullString()))))
                                    .resultMatcher(
                                            jsonPath(
                                                    "$.messages.*",
                                                    contains(
                                                            "The citation id has invalid format. It should be a PubMedId (number) or with CI-\\w{1,13} or IND[0-9]+")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe citation id has invalid format. It should be a PubMedId (number) or with CI-\\w{1,13} or IND[0-9]+")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            is(
                                                                    "Error messages\nThe citation id has invalid format. It should be a PubMedId (number) or with CI-\\w{1,13} or IND[0-9]+")))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                                    .resultMatcher(
                                            content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                                    .build())
                    .contentTypeParam(
                            ContentTypeParam.builder()
                                    .contentType(UniProtMediaType.RDF_MEDIA_TYPE)
                                    .resultMatcher(
                                            content()
                                                    .string(
                                                            containsString(
                                                                    "The citation id has invalid format")))
                                    .build())
                    .build();
        }
    }
}
