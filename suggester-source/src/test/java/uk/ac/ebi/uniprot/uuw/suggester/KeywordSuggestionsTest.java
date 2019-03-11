package uk.ac.ebi.uniprot.uuw.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion.computeWeightForName;

/**
 * Created 03/10/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class KeywordSuggestionsTest {
    private KeywordSuggestions keywordSuggestions;

    @Mock
    private List<String> suggestions;

    @Before
    public void setUp() {
        this.keywordSuggestions = new KeywordSuggestions();
    }

    @Test
    public void givenIDAndACValue_whenProcess_thenWriteSuggestion() {
        String kwId = "Abscisic acid signaling pathway";
        String id = "938";
        String kwAc = "KW-0" + id;

        Suggestion.SuggestionBuilder suggestionBuilder = Suggestion.builder();
        suggestionBuilder = keywordSuggestions.process("ID   " + kwId, suggestionBuilder, suggestions);
        verify(suggestions, times(0)).add(anyString());

        suggestionBuilder = keywordSuggestions.process("AC   " + kwAc, suggestionBuilder, suggestions);
        verify(suggestions, times(0)).add(anyString());

        suggestionBuilder = keywordSuggestions.process("ANY OTHER LINE THAT IS NOT '//'", suggestionBuilder, suggestions);
        verify(suggestions, times(0)).add(anyString());

        keywordSuggestions.process("//", suggestionBuilder, suggestions);
        Suggestion expectedSuggestion = Suggestion.builder().name(kwId).id(id).weight(computeWeightForName(kwId)).build();
        verify(suggestions, times(1)).add(expectedSuggestion.toSuggestionLine());
    }
}