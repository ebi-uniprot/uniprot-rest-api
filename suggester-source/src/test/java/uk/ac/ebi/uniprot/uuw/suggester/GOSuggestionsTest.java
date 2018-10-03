package uk.ac.ebi.uniprot.uuw.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.PrintWriter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created 03/10/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class GOSuggestionsTest {
    private GOSuggestions goSuggestions;

    @Mock
    private PrintWriter out;

    @Before
    public void setUp() {
        this.goSuggestions = new GOSuggestions();
    }

    @Test
    public void givenGOLine_whenProcess_thenGenerateExpectedSuggestion() {
        String goId = "GO:0051536";
        String goName = "iron-sulfur cluster binding";
        String input = "A0A007    DR   GO; " + goId + "; F:" + goName + "; IEA:InterPro.";

        goSuggestions.process(input, out);

        String suggestionLine = Suggestion.builder().id(goId).name(goName).build().toSuggestionLine();
        verify(out, times(1)).println(suggestionLine);
    }

    @Test
    public void givenGOLineWithWrongDB_whenProcess_thenGenerateNothing() {
        String input = "A0A007   DR   GO; " + "ECO:0051536" + "; F:" + "iron-sulfur cluster binding" + "; IEA:InterPro.";

        goSuggestions.process(input, out);

        verify(out, times(0)).println(anyString());
    }
}