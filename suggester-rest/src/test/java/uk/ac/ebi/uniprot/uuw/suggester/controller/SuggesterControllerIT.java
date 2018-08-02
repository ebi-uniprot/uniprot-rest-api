package uk.ac.ebi.uniprot.uuw.suggester.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestions;
import uk.ac.ebi.uniprot.uuw.suggester.service.SuggesterService;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.ac.ebi.uniprot.uuw.suggester.SuggestionDictionary.taxonomy;
import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestions.createSuggestions;


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
        List<String> results = asList("some text 1", "some text 2");
        Suggestions suggestions = createSuggestions(taxonomy, query, results);
        given(suggesterService.getSuggestions(taxonomy, query)).willReturn(suggestions);

        mockMvc.perform(get("/suggester")
                                .param("dict", "taxonomy")
                                .param("query", query))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query", is(query)))
                .andExpect(jsonPath("$.dictionary", is(taxonomy.name())))
                .andExpect(jsonPath("$.suggestions.*", is(results)));
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