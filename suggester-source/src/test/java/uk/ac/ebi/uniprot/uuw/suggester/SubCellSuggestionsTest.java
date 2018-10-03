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
public class SubCellSuggestionsTest {
    private SubCellSuggestions subCellSuggestions;

    @Mock
    private PrintWriter out;

    @Before
    public void setUp() {
        this.subCellSuggestions = new SubCellSuggestions();
    }

    @Test
    public void givenIDAndACValue_whenProcess_thenWriteSuggestion() {
        String subcellId = "Z line";
        String subcellAc = "SL-0314";

        Suggestion.SuggestionBuilder suggestionBuilder = Suggestion.builder();
        suggestionBuilder = subCellSuggestions.process("ID   " + subcellId, suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        suggestionBuilder = subCellSuggestions.process("AC   " + subcellAc, suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        suggestionBuilder = subCellSuggestions.process("ANY OTHER LINE THAT IS NOT '//'", suggestionBuilder, out);
        verify(out, times(0)).println(anyString());

        subCellSuggestions.process("//", suggestionBuilder, out);
        Suggestion expectedSuggestion = Suggestion.builder().name(subcellId).id(subcellAc).build();
        verify(out, times(1)).println(expectedSuggestion.toSuggestionLine());
    }
}