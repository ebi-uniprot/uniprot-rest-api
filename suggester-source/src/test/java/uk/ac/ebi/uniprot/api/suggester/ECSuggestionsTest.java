package uk.ac.ebi.uniprot.api.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ebi.uniprot.api.suggester.ECSuggestions;
import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;
import uk.ac.ebi.uniprot.cv.ec.EC;
import uk.ac.ebi.uniprot.cv.ec.impl.ECImpl;

import java.io.PrintWriter;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

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
    public void writeSuggestionsInCorrectOrder() {
        EC ecA9 = new ECImpl.Builder().label("AAAA").id("9999").build();
        EC ecB8 = new ECImpl.Builder().label("BBBB").id("8888").build();
        EC ecB0 = new ECImpl.Builder().label("BBBB").id("0000").build();
        List<EC> ecs = asList(ecB8, ecA9, ecB0);

        ecSuggestions.writeSuggestionsToOutputStream(ecs, out);

        verify(out).println(Suggestion.builder()
                                    .id(ecA9.id())
                                    .name(ecA9.label())
                                    .weight(computeWeightForName(ecA9.label()))
                                    .build()
                                    .toSuggestionLine());
        verify(out).println(Suggestion.builder()
                                    .id(ecB0.id())
                                    .name(ecB0.label())
                                    .weight(computeWeightForName(ecB0.label()))
                                    .build()
                                    .toSuggestionLine());
        verify(out).println(Suggestion.builder()
                                    .id(ecB8.id())
                                    .name(ecB8.label())
                                    .weight(computeWeightForName(ecB8.label()))
                                    .build()
                                    .toSuggestionLine());
    }

    @Test
    public void canGetECs() {
        List<EC> ecs = ECSuggestions.getECs();
        assertThat(ecs, hasSize(greaterThan(0)));
    }
}