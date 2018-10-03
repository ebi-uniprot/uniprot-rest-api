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
public class KeywordSuggestionsTest {
    private KeywordSuggestions keywordSuggestions;

    @Mock
    private PrintWriter out;

    @Before
    public void setUp() {
        this.keywordSuggestions = new KeywordSuggestions();
    }

    @Test
    public void givenIDAndACValue_whenProcess_thenWriteSuggestion() {
        String kwId = "Abscisic acid signaling pathway";
        String kwAc = "KW-0938";

        Suggestion.SuggestionBuilder suggestionBuilder = Suggestion.builder();
        suggestionBuilder = keywordSuggestions.process("ID   " + kwId, suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        suggestionBuilder = keywordSuggestions.process("AC   " + kwAc, suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        suggestionBuilder = keywordSuggestions.process("ANY OTHER LINE THAT IS NOT '//'", suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        keywordSuggestions.process("//", suggestionBuilder, out);
        Suggestion expectedSuggestion = Suggestion.builder().name(kwId).id(kwAc).build();
        verify(out, times(1)).println(expectedSuggestion.toSuggestionLine());
    }
}