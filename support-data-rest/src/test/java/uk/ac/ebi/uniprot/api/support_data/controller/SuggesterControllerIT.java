package uk.ac.ebi.uniprot.api.support_data.controller;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.repository.SolrTestConfig;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDictionary;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDocument;
import uk.ac.ebi.uniprot.search.field.SuggestField;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.search.document.suggest.SuggestDictionary.*;
import static uk.ac.ebi.uniprot.search.field.SuggestField.Importance.medium;

/**
 * Created 19/05/19
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SolrTestConfig.class, SupportDataApplication.class})
@WebAppConfiguration
public class SuggesterControllerIT {
    private static final String SEARCH_RESOURCE = "/suggester";

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws IOException, SolrServerException {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();

        populateIndexWithDocs();
    }

    private void populateIndexWithDocs() throws IOException, SolrServerException {
        saveSuggestionDoc("myId", "myValue", asList("one", "two", "three"), MAIN, medium);
        saveSuggestionDoc("myId", "myValue", asList("one", "two"), GO, medium);
        saveSuggestionDoc("myId", "myValue", singletonList("one"), EC, medium);
        saveSuggestionDoc("9606", "Homo sapiens", asList("Man", "Human"), TAXONOMY, medium);
    }

    @Test
    public void findsSpecificSuggestion() throws Exception {
        // given
        String id = "myId";
        saveSuggestionDoc(id, "myValue", emptyList());

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("dict", SuggestDictionary.TAXONOMY.name())
                        .param("query", id));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", contains(id)));
    }

    @Test
    public void findsBothSuggestions() throws Exception {
        // given
        String id1 = "myId1";
        String id2 = "myId2";
        saveSuggestionDoc(id1, "brown dog", emptyList());
        saveSuggestionDoc(id2, "black dog", emptyList());

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("dict", SuggestDictionary.TAXONOMY.name())
                        .param("query", "dog"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", containsInAnyOrder(id1, id2)));
    }

    @Test
    public void findsNoSuggestions() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("dict", SuggestDictionary.TAXONOMY.name())
                        .param("query", "XXXXXXXXXXXX"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.suggestions.*.id", is(empty())));
    }

    @Test
    public void missingRequiredDictField() throws Exception {
        // when
        String requiredParam = "dict";
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "XXXXXXXXXXXX"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString(requiredParam)));
    }

    @Test
    public void missingRequiredQueryField() throws Exception {
        // when
        String requiredParam = "query";
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("dict", TAXONOMY.name()));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString(requiredParam)));
    }

    @Test
    public void unknownDictionaryCausesBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, APPLICATION_JSON_VALUE)
                        .param("query", "anything")
                        .param("dict", "INVALID_DICTIONARY"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", containsString("Unknown dictionary")));
    }

    private void saveSuggestionDoc(String id, String value, List<String> altValues) throws IOException, SolrServerException {
        saveSuggestionDoc(id, value, altValues, SuggestDictionary.TAXONOMY, SuggestField.Importance.medium);
    }

    private void saveSuggestionDoc(String id, String value, List<String> altValues, SuggestDictionary dict, SuggestField.Importance importance) throws IOException, SolrServerException {
        SolrClient solrClient = solrTemplate.getSolrClient();
        String collection = SolrCollection.suggest.name();
        solrClient.addBean(collection, SuggestDocument.builder()
                .dictionary(dict.name())
                .id(id)
                .value(value)
                .altValues(altValues)
                .importance(importance.name())
                .build());
        solrClient.commit(collection);
    }
}