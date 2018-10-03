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
public class ECSuggestionsTest {
    private ECSuggestions ecSuggestions;

    @Mock
    private PrintWriter out;

    @Before
    public void setUp() {
        this.ecSuggestions = new ECSuggestions();
    }

    @Test
    public void givenIDAndDEValue_whenProcess_thenWriteSuggestion() {
        String ec = "1.1.1.1";
        String ecName = "Alcohol dehydrogenase.";

        Suggestion.SuggestionBuilder suggestionBuilder = Suggestion.builder();
        suggestionBuilder = ecSuggestions.process("ID   " + ec, suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        suggestionBuilder = ecSuggestions.process("DE   " + ecName, suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        suggestionBuilder = ecSuggestions.process("ANY OTHER LINE THAT IS NOT '//'", suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        ecSuggestions.process("//", suggestionBuilder, out);
        Suggestion expectedSuggestion = Suggestion.builder().name(ecName).id(ec).build();
        verify(out, times(1)).println(expectedSuggestion.toSuggestionLine());
    }

}