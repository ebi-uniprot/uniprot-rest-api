package org.uniprot.api.support.data.suggester.controller;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.store.search.document.suggest.SuggestDictionary.*;
import static org.uniprot.store.search.field.SuggestField.Importance.*;

import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.suggester.service.SuggesterService;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.suggest.SuggestDictionary;
import org.uniprot.store.search.document.suggest.SuggestDocument;
import org.uniprot.store.search.field.SuggestField;

/**
 * Created 19/05/19
 *
 * @author Edd
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuggesterControllerIT {
    private static final String SEARCH_RESOURCE = "/suggester";

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired SuggesterService suggesterService;

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(DataStoreManager.StoreType.SUGGEST, SolrCollection.suggest);
        ReflectionTestUtils.setField(
                suggesterService,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.SUGGEST));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        populateIndexWithDocs();
    }

    @AfterEach
    void tearDown() {
        storeManager.cleanStore(DataStoreManager.StoreType.SUGGEST);
    }

    private void populateIndexWithDocs() {
        saveSuggestionDoc("myId", "myValue", asList("one", "two", "three"), MAIN, medium);
        saveSuggestionDoc("myId", "myValue", asList("one", "two"), GO, medium);
        saveSuggestionDoc("myId", "myValue", singletonList("one"), EC, medium);
        saveSuggestionDoc("9606", "Homo sapiens", asList("Man", "Human"), TAXONOMY, medium);
    }

    @Test
    void findsSpecificSuggestion() throws Exception {
        // given
        String id = "myId";
        saveSuggestionDoc(id, "myValue", emptyList());

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", SuggestDictionary.TAXONOMY.name())
                                .param("query", id));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", contains(id)));
    }

    @Test
    void findsBothSuggestions() throws Exception {
        // given
        String id1 = "myId1";
        String id2 = "myId2";
        saveSuggestionDoc(id1, "brown dog", emptyList());
        saveSuggestionDoc(id2, "black dog", emptyList());

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", SuggestDictionary.TAXONOMY.name())
                                .param("query", "dog"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", containsInAnyOrder(id1, id2)));
    }

    @Test
    void findsNoSuggestions() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", SuggestDictionary.TAXONOMY.name())
                                .param("query", "XXXXXXXXXXXX"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", is(empty())));
    }

    @Test
    void importanceOrderTest() throws Exception {
        // given
        saveSuggestionDoc("1", "order value 1", singletonList("one"), EC, medium);
        saveSuggestionDoc("2", "order value 1", singletonList("two"), EC, low);
        saveSuggestionDoc("3", "order value 1", singletonList("three"), EC, highest);
        saveSuggestionDoc("4", "order value 1", singletonList("four"), EC, high);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", EC.name())
                                .param("query", "order"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", containsInAnyOrder("3", "4", "1", "2")));
    }

    @Test
    void lowestImportanceShouldBeAtEnd() throws Exception {
        // given
        saveSuggestionDoc("1", "any data", singletonList("two"), EC, low);
        saveSuggestionDoc("2", "any data", singletonList("one"), EC, medium);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", EC.name())
                                .param("query", "any data"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", containsInAnyOrder("2", "1")));
    }

    @Test
    void missingRequiredDictField() throws Exception {
        // when
        String requiredParam = "dict";
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "XXXXXXXXXXXX"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", hasItem(containsString(requiredParam))));
    }

    @Test
    void missingRequiredQueryField() throws Exception {
        // when
        String requiredParam = "query";
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", TAXONOMY.name()));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", hasItem(containsString(requiredParam))));
    }

    @Test
    void unknownDictionaryCausesBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "anything")
                                .param("dict", "INVALID_DICTIONARY"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", hasItem(containsString("Unknown dictionary"))));
    }

    @Test
    void suggestDisease() throws Exception {
        // given
        String id = "DI-00001";
        saveSuggestionDoc(
                id,
                "Lung Cancer",
                List.of("Adenocarcinoma of lung", "Alveolar cell carcinoma"),
                DISEASE,
                medium);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", DISEASE.name())
                                .param("query", "lung"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", contains(id)));
    }

    @Test
    void suggestKeyword() throws Exception {
        // given
        String id = "KW-0005";
        saveSuggestionDoc(id, "Acetoin biosynthesis", List.of(), KEYWORD, medium);

        // when
        ResultActions response =
                mockMvc.perform(
                        get(SEARCH_RESOURCE)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("dict", KEYWORD.name())
                                .param("query", "Acet"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", contains(id)));
    }

    private void saveSuggestionDoc(String id, String value, List<String> altValues) {
        saveSuggestionDoc(
                id, value, altValues, SuggestDictionary.TAXONOMY, SuggestField.Importance.medium);
    }

    private void saveSuggestionDoc(
            String id,
            String value,
            List<String> altValues,
            SuggestDictionary dict,
            SuggestField.Importance importance) {
        SuggestDocument doc =
                SuggestDocument.builder()
                        .dictionary(dict.name())
                        .id(id)
                        .value(value)
                        .altValues(altValues)
                        .importance(importance.name())
                        .build();
        storeManager.saveDocs(DataStoreManager.StoreType.SUGGEST, doc);
    }
}
