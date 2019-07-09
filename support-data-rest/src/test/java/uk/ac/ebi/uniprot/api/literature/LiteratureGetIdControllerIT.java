package uk.ac.ebi.uniprot.api.literature;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.domain.citation.impl.AuthorImpl;
import uk.ac.ebi.uniprot.domain.citation.impl.PublicationDateImpl;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.domain.literature.builder.LiteratureEntryBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.literature.LiteratureJsonConfig;
import uk.ac.ebi.uniprot.search.document.literature.LiteratureDocument;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author lgonzales
 * @since 2019-07-05
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(LiteratureController.class)
@ExtendWith(value = {SpringExtension.class, LiteratureGetIdControllerIT.LiteratureGetIdParameterResolver.class,
        LiteratureGetIdControllerIT.LiteratureGetIdContentTypeParamResolver.class})
class LiteratureGetIdControllerIT extends AbstractGetByIdControllerIT {

    private static final long PUBMED_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Override
    protected void saveEntry() {
        LiteratureEntry literatureEntry = new LiteratureEntryBuilder()
                .pubmedId(PUBMED_ID)
                .title("The Title")
                .addAuthor(new AuthorImpl("The Author"))
                .literatureAbstract("literature abstract")
                .publicationDate(new PublicationDateImpl("2019"))
                .firstPage("10")
                .build();

        LiteratureDocument document = LiteratureDocument.builder()
                .id(String.valueOf(PUBMED_ID))
                .literatureObj(getLiteratureBinary(literatureEntry))
                .build();

        storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getIdRequestPath() {
        return "/literature/";
    }

    private ByteBuffer getLiteratureBinary(LiteratureEntry entry) {
        try {
            return ByteBuffer.wrap(LiteratureJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }

    static class LiteratureGetIdParameterResolver extends AbstractGetIdParameterResolver {

        @Override
        public GetIdParameter validIdParameter() {
            return GetIdParameter.builder().id(String.valueOf(PUBMED_ID))
                    .resultMatcher(jsonPath("$.pubmedId", is(100)))
                    .resultMatcher(jsonPath("$.authors", contains("The Author")))
                    .resultMatcher(jsonPath("$.title", is("The Title")))
                    .build();
        }

        @Override
        public GetIdParameter invalidIdParameter() {
            return GetIdParameter.builder().id("INVALID")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("The PubMed id value should be a number")))
                    .build();
        }

        @Override
        public GetIdParameter nonExistentIdParameter() {
            return GetIdParameter.builder().id("999")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Resource not found")))
                    .build();
        }

        @Override
        public GetIdParameter withFilterFieldsParameter() {
            return GetIdParameter.builder().id(String.valueOf(PUBMED_ID)).fields("id,title")
                    .resultMatcher(jsonPath("$.pubmedId", is(100)))
                    .resultMatcher(jsonPath("$.title", is("The Title")))
                    .resultMatcher(jsonPath("$.authors").doesNotExist())
                    .build();
        }

        @Override
        public GetIdParameter withInvalidFilterParameter() {
            return GetIdParameter.builder().id(String.valueOf(PUBMED_ID)).fields("invalid")
                    .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
                    .build();
        }
    }

    static class LiteratureGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

        @Override
        public GetIdContentTypeParam idSuccessContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id(String.valueOf(PUBMED_ID))
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.pubmedId", is(100)))
                            .resultMatcher(jsonPath("$.authors", contains("The Author")))
                            .resultMatcher(jsonPath("$.title", is("The Title")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString(String.valueOf(PUBMED_ID))))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("PubMed ID\tTitle\tReference\tAbstract/Summary")))
                            .resultMatcher(content().string(containsString("100\tThe Title\t10(2019)\tliterature abstract")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                            .build())
                    .build();
        }

        @Override
        public GetIdContentTypeParam idBadRequestContentTypesParam() {
            return GetIdContentTypeParam.builder()
                    .id("INVALID")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url", not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*", contains("The PubMed id value should be a number")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .build();
        }
    }

}