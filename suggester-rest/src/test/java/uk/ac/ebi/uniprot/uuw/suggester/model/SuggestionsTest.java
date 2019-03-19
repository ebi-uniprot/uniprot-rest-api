package uk.ac.ebi.uniprot.uuw.suggester.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.uuw.suggester.SuggestionDictionary;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestions.*;

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
        String suggestion = suggestionId + " " + ID_VALUE_SEPARATOR + " " + suggestionValue;

        Suggestions suggestions = createSuggestions(SuggestionDictionary.ec, "anything", singletonList(suggestion));

        Suggestion expectedSuggestion = new Suggestion();
        expectedSuggestion.setValue(suggestionValue);
        expectedSuggestion.setId(suggestionId);
        assertThat(suggestions.getSuggestions(), is(singletonList(expectedSuggestion)));
    }

    @Test
    void canCreateSuggestionWithNamePartsAndId() {
        String altName = "alternative-name";
        String sciName = "scientific-name";
        String suggestionValue = altName + VALUE_DELIMITER + sciName;
        String suggestionId = "11111";
        String suggestion = suggestionId + " " + ID_VALUE_SEPARATOR + " " + suggestionValue;

        Suggestions suggestions = createSuggestions(SuggestionDictionary.ec, "anything", singletonList(suggestion));

        Suggestion expectedSuggestion = new Suggestion();
        expectedSuggestion.setValue(sciName + " (" + altName + ")");
        expectedSuggestion.setId(suggestionId);
        assertThat(suggestions.getSuggestions(), is(singletonList(expectedSuggestion)));
    }
}