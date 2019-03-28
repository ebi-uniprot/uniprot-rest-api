package uk.ac.ebi.uniprot.api.suggester.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SuggesterResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ebi.uniprot.api.suggester.SuggestionDictionary;
import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;
import uk.ac.ebi.uniprot.api.suggester.model.Suggestions;
import uk.ac.ebi.uniprot.api.suggester.service.SuggesterService;
import uk.ac.ebi.uniprot.api.suggester.service.SuggestionRetrievalException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.uniprot.api.suggester.SuggestionDictionary.taxonomy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;



/**
 * Created 18/07/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class SuggesterServiceTest {
    @Mock
    private SolrClient solrClient;
    private SuggesterService suggesterService;

    @Before
    public void setUp() {
        this.suggesterService = new SuggesterService(solrClient, "anyCollection");
    }

    @Test
    public void createsSuggestionsWhenSolrFindsThemSuccessfully() throws IOException, SolrServerException {
        SuggestionDictionary dict = taxonomy;
        String query = "any string";
        List<String> suggestions = asList("suggestion 1", "suggestion 2");
        List<Suggestion> expectedSuggestions = asSuggestionList(suggestions);

        String dictStr = dict.name();
        mockServiceQueryResponse(dict, suggestions);

        Suggestions retrievedSuggestions = suggesterService.getSuggestions(dict, query);
        assertThat(retrievedSuggestions.getDictionary(), is(dictStr));
        assertThat(retrievedSuggestions.getQuery(), is(query));
        assertThat(retrievedSuggestions.getSuggestions(), is(expectedSuggestions));
    }

    private List<Suggestion> asSuggestionList(List<String> suggestionStrings) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (String suggestionString : suggestionStrings) {
            Suggestion suggestion = new Suggestion();
            suggestion.setValue(suggestionString);
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @Test
    public void createsSuggestionsWithoutDuplicatesWhenSolrFindsThemSuccessfully() throws IOException, SolrServerException {
        SuggestionDictionary dict = taxonomy;
        String query = "any string";
        List<String> suggestions = asList("suggestion 1", "suggestion 2", "suggestion 1");
        List<Suggestion> suggestionsWithoutDuplicates = asSuggestionList(asList("suggestion 1", "suggestion 2"));

        String dictStr = dict.name();
        mockServiceQueryResponse(dict, suggestions);

        Suggestions retrievedSuggestions = suggesterService.getSuggestions(dict, query);
        assertThat(retrievedSuggestions.getDictionary(), is(dictStr));
        assertThat(retrievedSuggestions.getQuery(), is(query));
        assertThat(retrievedSuggestions.getSuggestions(), is(suggestionsWithoutDuplicates));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = SuggestionRetrievalException.class)
    public void suggestionThatCausesSolrExceptionCausesSuggestionRetrievalException() throws IOException, SolrServerException {
        when(solrClient.query(anyString(), any())).thenThrow(SolrServerException.class);
        suggesterService.getSuggestions(taxonomy, "some text");
    }

    private void mockServiceQueryResponse(SuggestionDictionary dict, List<String> suggestions) throws SolrServerException, IOException {
        Map<String, List<String>> suggestionMap = new HashMap<>();
        suggestionMap.put(dict.getId(), suggestions);
        QueryResponse queryResponse = mock(QueryResponse.class);
        SuggesterResponse suggesterResponse = mock(SuggesterResponse.class);
        when(queryResponse.getSuggesterResponse()).thenReturn(suggesterResponse);
        when(suggesterResponse.getSuggestedTerms()).thenReturn(suggestionMap);
        when(solrClient.query(anyString(), any())).thenReturn(queryResponse);
    }
}