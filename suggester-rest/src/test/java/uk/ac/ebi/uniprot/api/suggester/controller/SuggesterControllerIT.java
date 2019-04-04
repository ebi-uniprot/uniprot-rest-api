package uk.ac.ebi.uniprot.api.suggester.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.ebi.uniprot.api.suggester.model.Suggestions;
import uk.ac.ebi.uniprot.api.suggester.service.SuggesterService;
import uk.ac.ebi.uniprot.api.support_data.controller.SuggesterController;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.uniprot.api.suggester.SuggestionDictionary.taxonomy;
import static uk.ac.ebi.uniprot.api.suggester.model.Suggestions.ID_VALUE_SEPARATOR;
import static uk.ac.ebi.uniprot.api.suggester.model.Suggestions.createSuggestions;


/**
 * Created 18/07/18
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@WebMvcTest(SuggesterController.class)
public class SuggesterControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuggesterService suggesterService;

    @Test
    public void canRetrieveSuggestionsOkay() throws Exception {
        String query = "some text";
        String value1 = "some text 1";
        String value2 = "some text 2";
        String id1 = "1234";
        List<String> results = asList(id1 + " " + ID_VALUE_SEPARATOR + " " + value1, value2);
        Suggestions suggestions = createSuggestions(taxonomy, query, results);
        given(suggesterService.getSuggestions(taxonomy, query)).willReturn(suggestions);


        mockMvc.perform(get("/suggester")
                                .param("dict", "taxonomy")
                                .param("query", query))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is(query)))
                .andExpect(jsonPath("$.dictionary", is(taxonomy.name())))
                .andExpect(jsonPath("$.suggestions[0].value", is(value1)))
                .andExpect(jsonPath("$.suggestions[0].id", is(id1)))
                .andExpect(jsonPath("$.suggestions[1].value", is(value2)))
                .andExpect(jsonPath("$.suggestions[1].id", is(nullValue())));
    }

    @Test
    public void requestMustIncludeDictParam() throws Exception {
        mockMvc.perform(get("/suggester")
                                .param("query", "anything"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void requestMustIncludeQueryParam() throws Exception {
        mockMvc.perform(get("/suggester")
                                .param("dict", "anything"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void unknownDictionaryCausesBadRequest() throws Exception {
        String query = "some text";
        given(suggesterService.getSuggestions(taxonomy, query))
                .willReturn(createSuggestions(taxonomy, query, asList("some text 1", "some text 2")));

        mockMvc.perform(get("/suggester")
                                .param("dict", "WRONG_DICTIONARY")
                                .param("query", query))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}