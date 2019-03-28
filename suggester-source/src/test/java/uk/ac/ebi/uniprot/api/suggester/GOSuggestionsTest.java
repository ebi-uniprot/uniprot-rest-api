package uk.ac.ebi.uniprot.api.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ebi.uniprot.api.suggester.GOSuggestions;
import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

/**
 * Created 03/10/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class GOSuggestionsTest {
    private GOSuggestions goSuggestions;
    private Set<String> suggestionsSet;

    @Before
    public void setUp() {
        this.goSuggestions = new GOSuggestions();
        suggestionsSet = new HashSet<>();
    }

    @Test
    public void givenGOLine_whenProcess_thenGenerateExpectedSuggestion() {
        String id = "51536";
        String goId = "GO:00" + id;
        String goName = "iron-sulfur cluster binding";
        String input = " " + goId + " F:" + goName;

        goSuggestions.process(input, suggestionsSet);

        String suggestionLine = Suggestion.builder().id(id).name(goName).weight(computeWeightForName(goName)).build().toSuggestionLine();
        assertThat(suggestionsSet, contains(suggestionLine));
    }

    @Test
    public void givenGOLineWithWrongDB_whenProcess_thenGenerateNothing() {
        String input = "A0A007   DR   GO; " + "ECO:0051536" + "; F:" + "iron-sulfur cluster binding" + "; IEA:InterPro.";

        goSuggestions.process(input, suggestionsSet);

        assertThat(suggestionsSet, hasSize(0));
    }
}