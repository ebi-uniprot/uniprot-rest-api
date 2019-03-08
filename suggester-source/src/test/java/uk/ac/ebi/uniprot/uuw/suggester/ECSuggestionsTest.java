package uk.ac.ebi.uniprot.uuw.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
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
public class ECSuggestionsTest {
    private ECSuggestions ecSuggestions;

    @Mock
    private Collection<String> out;

    @Before
    public void setUp() {
        this.ecSuggestions = new ECSuggestions();
    }

    @Test
    public void givenIDAndDEValue_whenProcess_thenWriteSuggestion() {
        String ec = "1.1.1.1";
        String ecName = "Alcohol dehydrogenase.";

        Suggestion.SuggestionBuilder suggestionBuilder = Suggestion.builder();
        suggestionBuilder = ecSuggestions.processEnzymeDatLine("ID   " + ec, suggestionBuilder, out);
        verify(out, times(0)).add(anyString());

        suggestionBuilder = ecSuggestions.processEnzymeDatLine("DE   " + ecName, suggestionBuilder, out);
        verify(out, times(0)).add(anyString());

        suggestionBuilder = ecSuggestions
                .processEnzymeDatLine("ANY OTHER LINE THAT IS NOT '//'", suggestionBuilder, out);
        verify(out, times(0)).add(anyString());

        ecSuggestions.processEnzymeDatLine("//", suggestionBuilder, out);
        Suggestion expectedSuggestion = Suggestion.builder().name(ecName).id(ec).weight(computeWeightForName(ecName))
                .build();
        verify(out, times(1)).add(expectedSuggestion.toSuggestionLine());
    }

    @Test
    public void givenFullLineFromClassFile_whenProcess_thenWriteSuggestion() {
        String line = ecSuggestions.processEnzymeClassLine("1. 1. 9.-    With a copper protein as acceptor.");
        String name = "With a copper protein as acceptor";
        assertThat(line, is(Suggestion.builder().id("1.1.9.-").name(name).weight(computeWeightForName(name)).build()
                                    .toSuggestionLine()));
    }

    @Test
    public void givenIrrelavantFullLineFromClassFile_whenProcess_thenWriteSuggestion() {
        assertThat(ecSuggestions.processEnzymeClassLine("------------------"), is(nullValue()));
    }
}