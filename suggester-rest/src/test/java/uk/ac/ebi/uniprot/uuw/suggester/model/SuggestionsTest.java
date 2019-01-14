package uk.ac.ebi.uniprot.uuw.suggester.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.uuw.suggester.SuggestionDictionary;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestions.createSuggestions;

/**
 * Created 17/12/18
 *
 * @author Edd
 */
class SuggestionsTest {
    @Test
    void createSuggestionWithDictionary() {
        Suggestions suggestions = createSuggestions(SuggestionDictionary.ec, "anything", emptyList());
        assertThat(suggestions.getDictionary(), is("ec"));
    }

    @Test
    void createSuggestionWithQuery() {
        String query = "my special query";
        Suggestions suggestions = createSuggestions(SuggestionDictionary.ec, query, emptyList());
        assertThat(suggestions.getQuery(), is(query));
    }

    @Test
    void canCreateSuggestionWithNoId() {
        String suggestion = "some suggestion without id";

        Suggestions suggestions = createSuggestions(SuggestionDictionary.ec, "anything", singletonList(suggestion));

        Suggestion expectedSuggestion = new Suggestion();
        expectedSuggestion.setValue(suggestion);
        assertThat(suggestions.getSuggestions(), is(singletonList(expectedSuggestion)));
    }

    @Test
    void canCreateSuggestionWithId() {
        String suggestionValue = "some suggestion with id";
        String suggestionId = "GO:0000001";
        String suggestion = suggestionValue + " [" + suggestionId + "]";

        Suggestions suggestions = createSuggestions(SuggestionDictionary.ec, "anything", singletonList(suggestion));

        Suggestion expectedSuggestion = new Suggestion();
        expectedSuggestion.setValue(suggestionValue);
        expectedSuggestion.setId(suggestionId);
        assertThat(suggestions.getSuggestions(), is(singletonList(expectedSuggestion)));
    }
}