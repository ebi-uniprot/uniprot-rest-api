package org.uniprot.api.common.repository.search.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SuggestionConverterTest {

    private SuggestionConverter converter;
    private QueryResponse mockQueryResponse;
    private SpellCheckResponse mockSpellCheckResponse;

    @BeforeEach
    void setUp() {
        converter = new SuggestionConverter();
        mockQueryResponse = mock(QueryResponse.class);
        mockSpellCheckResponse = mock(SpellCheckResponse.class);

        when(mockQueryResponse.getSpellCheckResponse()).thenReturn(mockSpellCheckResponse);
    }

    @Test
    void convertsNullSolrSuggestionsToEmptyListOfUniProtSuggestions() {
        // given --------------------------------------------------------------
        simulateSuggestionsReturnedAre(null);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(0));
    }

    @Test
    void convertsSingleSolrSuggestionToSingletonListOfUniProtSuggestions() {
        // given --------------------------------------------------------------
        String token = "bell";
        List<String> alternatives = List.of("bill");
        List<Integer> alternativesFrequencies = List.of(1);
        simulateSuggestionsReturnedAre(new Object[] {token, alternatives, alternativesFrequencies});

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).getOriginal(), is(token));
        assertThat(
                suggestions.get(0).getAlternatives().stream()
                        .map(Alternative::getTerm)
                        .collect(Collectors.toList()),
                is(alternatives));
        assertThat(
                suggestions.get(0).getAlternatives().stream()
                        .map(Alternative::getCount)
                        .collect(Collectors.toList()),
                is(alternativesFrequencies));
    }

    @Test
    void convertsMultipleSolrSuggestionsToMultipleUniProtSuggestions() {
        // given --------------------------------------------------------------
        String token0 = "bell";
        List<String> alternatives0 = List.of("bill", "ball");
        List<Integer> alternativesFrequencies0 = List.of(1, 8);

        String token1 = "shop";
        List<String> alternatives1 = List.of("ship");
        List<Integer> alternativesFrequencies1 = List.of(2);

        simulateSuggestionsReturnedAre(
                new Object[] {token0, alternatives0, alternativesFrequencies0},
                new Object[] {token1, alternatives1, alternativesFrequencies1});

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(2));
        assertThat(suggestions.get(0).getOriginal(), is(token0));
        assertThat(
                suggestions.get(0).getAlternatives().stream()
                        .map(Alternative::getTerm)
                        .collect(Collectors.toList()),
                is(alternatives0));
        assertThat(
                suggestions.get(0).getAlternatives().stream()
                        .map(Alternative::getCount)
                        .collect(Collectors.toList()),
                is(alternativesFrequencies0));

        assertThat(suggestions.get(1).getOriginal(), is(token1));
        assertThat(
                suggestions.get(1).getAlternatives().stream()
                        .map(Alternative::getTerm)
                        .collect(Collectors.toList()),
                is(alternatives1));
        assertThat(
                suggestions.get(1).getAlternatives().stream()
                        .map(Alternative::getCount)
                        .collect(Collectors.toList()),
                is(alternativesFrequencies1));
    }

    @SuppressWarnings("unchecked")
    private void simulateSuggestionsReturnedAre(Object[]... triples) {
        if (triples != null) {
            List<SpellCheckResponse.Suggestion> mockSuggestions = new ArrayList<>();

            for (Object[] triple : triples) {

                String token = (String) triple[0];
                List<String> alternatives = (List<String>) triple[1];
                List<Integer> alternativeFreqs = (List<Integer>) triple[2];

                SpellCheckResponse.Suggestion mockSuggestion =
                        mock(SpellCheckResponse.Suggestion.class);
                when(mockSuggestion.getToken()).thenReturn(token);
                when(mockSuggestion.getAlternatives()).thenReturn(alternatives);
                when(mockSuggestion.getAlternativeFrequencies()).thenReturn(alternativeFreqs);
                mockSuggestions.add(mockSuggestion);
            }
            when(mockSpellCheckResponse.getSuggestions()).thenReturn(mockSuggestions);
        } else {
            when(mockQueryResponse.getSpellCheckResponse()).thenReturn(null);
        }
    }
}
