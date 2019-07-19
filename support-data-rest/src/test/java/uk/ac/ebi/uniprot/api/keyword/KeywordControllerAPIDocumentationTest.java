package uk.ac.ebi.uniprot.api.keyword;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.constraints.ResourceBundleConstraintDescriptionResolver;
import org.springframework.restdocs.constraints.ValidatorConstraintResolver;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.keyword.request.KeywordRequestDTO;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordEntryImpl;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.keyword.KeywordJsonConfig;
import uk.ac.ebi.uniprot.search.document.keyword.KeywordDocument;

import java.nio.ByteBuffer;
import java.util.ResourceBundle;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.resourceDetails;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.uniprot.api.keyword.RRequestSnippet.rRequest;
import static uk.ac.ebi.uniprot.api.keyword.ResourceSnippet.resource;
import static uk.ac.ebi.uniprot.api.keyword.SupportedContentTypesSnippet.supportedContentTypes;

/**
 * E.g., check this https://github.com/ePages-de/restdocs-api-spec/blob/master/samples/restdocs-api-spec-sample/src/test/java/com/epages/restdocs/apispec/sample/ProductRestIntegrationTest.java
 * Created 28/06/19
 *
 * @author Edd
 */
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(KeywordController.class)
@AutoConfigureRestDocs
@ExtendWith(value = {RestDocumentationExtension.class, SpringExtension.class,
                     KeywordGetIdControllerIT.KeywordGetIdParameterResolver.class,
                     KeywordGetIdControllerIT.KeywordGetIdContentTypeParamResolver.class})
class KeywordControllerAPIDocumentationTest {
    private static final String KW_ACC = "KW-0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private DataStoreManager storeManager;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        MockMvcRestDocumentationConfigurer docConfigurer = documentationConfiguration(restDocumentation);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(docConfigurer)
                .apply(docConfigurer.snippets().withAdditionalDefaults(
                        rRequest(),
                        supportedContentTypes(requestMappingHandlerMapping),
                        resource(requestMappingHandlerMapping)))
                .apply(docConfigurer
                               .operationPreprocessors()
                               .withRequestDefaults(prettyPrint())
                               .withResponseDefaults(prettyPrint())
                )
                .build();
    }

    @Test
    void getAKeywordEntryById() throws Exception {
        String identifier = "get-keyword-by-id";

        saveEntry();
        this.mockMvc.perform(get("/keyword/{keywordId}", KW_ACC)
                                     .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(identifier,
                                resourceDetails()
                                        .description("Get a keyword entry by ID, which does not exist"),
                                pathParameters(
                                        parameterWithName("keywordId").description("the keyword id")
                                ),
                                responseFields(
                                        fieldWithPath("keyword.id").description("the keyword identifier"),
                                        fieldWithPath("keyword.accession").description("the keyword accession"),
                                        fieldWithPath("definition")
                                                .description("the keyword definition"),
                                        fieldWithPath("category.id").description("the keyword category identifier"),
                                        fieldWithPath("category.accession")
                                                .description("the keyword category accession")
                                )));
    }

    @Test
    void getKeywordByIdNotFound() throws Exception {
        String identifier = "get-keyword-by-id-not-found";

        this.mockMvc.perform(get("/keyword/{keywordId}", "KW-0000")
                                     .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andDo(document(identifier,
                                resourceDetails()
                                        .description("Attempt to get a keyword entry by ID, which does not exist"),
                                pathParameters(
                                        parameterWithName("keywordId").description("the keyword id")
                                ),
                                responseFields(
                                        fieldWithPath("url").description("the URL used that caused the error"),
                                        fieldWithPath("messages")
                                                .description("a list of messages that describe the problem")
                                )));
    }

    @Test
    void getKeywordsBySearch() throws Exception {
        String identifier = "get-keyword-by-search";

        ConstraintDescriptions keywordConstraints =
                new ConstraintDescriptions(KeywordRequestDTO.class, new ValidatorConstraintResolver(),
                                           new ResourceBundleConstraintDescriptionResolver(ResourceBundle
                                                                                                   .getBundle("constraint-descriptor")));
        saveEntry();
        this.mockMvc.perform(get("/keyword/search")
                                     .param("query", "keyword_id:KW-0001")
                                     .param("sort", "name desc")
                                     .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(identifier,
                                resourceDetails()
                                        .description("Find keywords that match a search query"),
                                requestParameters(
                                        attributes(key("title").value("There we go!!!")),
                                        parameterWithName("query")
                                                .description("the query")
                                                .attributes(
                                                        key("constraints").value(keywordConstraints
                                                                                         .descriptionsForProperty("query")))
                                        ,
                                        parameterWithName("sort")
                                                .description("Fields to sort on")
                                                .attributes(
                                                        key("constraints").value(keywordConstraints
                                                                                         .descriptionsForProperty("sort")))
                                ),
                                responseFields(
                                        subsectionWithPath("results")
                                                .description("A list of `keyword` objects matching the search query."),
                                        subsectionWithPath("facets")
                                                .description("A list of facets matching the search query.")
                                )));
    }

    @Test
    void getKeywordsBySearchUnsuccessful() throws Exception {
        String identifier = "get-keyword-by-search-unsuccessful";

        saveEntry();
        this.mockMvc.perform(get("/keyword/search")
                                     .param("sort", "name desc")
                                     .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andDo(document(identifier,
                                resourceDetails()
                                        .description("Request to find keywords is unsuccessful")));
    }

    private void saveEntry() {
        KeywordEntryImpl keywordEntry = new KeywordEntryImpl();
        keywordEntry.setDefinition("Protein which contains at least one 2Fe-2S iron-sulfur cluster: 2 iron atoms " +
                                           "complexed to 2 inorganic sulfides and 4 sulfur atoms of cysteines from " +
                                           "the protein");
        keywordEntry.setKeyword(new KeywordImpl("2Fe-2S", KW_ACC));
        keywordEntry.setCategory(new KeywordImpl("Ligand", "KW-9993"));

        KeywordDocument document = KeywordDocument.builder()
                .id(KW_ACC)
                .keywordObj(getKeywordBinary(keywordEntry))
                .build();

        storeManager.saveDocs(DataStoreManager.StoreType.KEYWORD, document);
    }

    private ByteBuffer getKeywordBinary(KeywordEntry entry) {
        try {
            return ByteBuffer.wrap(KeywordJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse KeywordEntry to binary json: ", e);
        }
    }
}
