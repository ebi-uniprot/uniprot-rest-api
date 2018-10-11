package uk.ac.ebi.uniprot.uuw.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

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
        String goId = "GO:0051536";
        String goName = "iron-sulfur cluster binding";
        String input = "A0A007    DR   GO; " + goId + "; F:" + goName + "; IEA:InterPro.";

        goSuggestions.process(input, suggestionsSet);

        String suggestionLine = Suggestion.builder().id(goId).name(goName).build().toSuggestionLine();
        assertThat(suggestionsSet, contains(suggestionLine));
    }

    @Test
    public void givenGOLineWithWrongDB_whenProcess_thenGenerateNothing() {
        String input = "A0A007   DR   GO; " + "ECO:0051536" + "; F:" + "iron-sulfur cluster binding" + "; IEA:InterPro.";

        goSuggestions.process(input, suggestionsSet);

        assertThat(suggestionsSet, hasSize(0));
    }

    @Test
    public void givenDuplicateGOLines_whenProcess_thenGenerateOnly1Suggestion() {
        String goId = "GO:0051536";
        String goName = "iron-sulfur cluster binding";
        String input1 = "AAAAAA    DR   GO; " + goId + "; F:" + goName + "; IEA:InterPro.";
        String input2 = "ZZZZZZ    DR   GO; " + goId + "; F:" + goName + "; IEA:InterPro.";

        goSuggestions.process(input1, suggestionsSet);
        goSuggestions.process(input2, suggestionsSet);

        String suggestionLine = Suggestion.builder().id(goId).name(goName).build().toSuggestionLine();
        assertThat(suggestionsSet, contains(suggestionLine));
    }
}