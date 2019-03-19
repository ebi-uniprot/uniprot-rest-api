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
public class SubCellSuggestionsTest {
    private SubCellSuggestions subCellSuggestions;

    @Mock
    private List<String> suggestions;

    @Before
    public void setUp() {
        this.subCellSuggestions = new SubCellSuggestions();
    }

    @Test
    public void givenIDAndACValue_whenProcess_thenWriteSuggestion() {
        String subcellId = "Z line";
        String id = "314";
        String subcellAc = "SL-0" + id;

        Suggestion.SuggestionBuilder suggestionBuilder = Suggestion.builder();
        suggestionBuilder = subCellSuggestions.process("ID   " + subcellId, suggestionBuilder, suggestions);
        verify(suggestions, times(0)).add(anyString());

        suggestionBuilder = subCellSuggestions.process("AC   " + subcellAc, suggestionBuilder, suggestions);
        verify(suggestions, times(0)).add(anyString());

        suggestionBuilder = subCellSuggestions
                .process("ANY OTHER LINE THAT IS NOT '//'", suggestionBuilder, suggestions);
        verify(suggestions, times(0)).add(anyString());

        subCellSuggestions.process("//", suggestionBuilder, suggestions);
        Suggestion expectedSuggestion = Suggestion.builder().name(subcellId).id(id)
                .weight(computeWeightForName(subcellId)).build();
        verify(suggestions, times(1)).add(expectedSuggestion.toSuggestionLine());
    }
}