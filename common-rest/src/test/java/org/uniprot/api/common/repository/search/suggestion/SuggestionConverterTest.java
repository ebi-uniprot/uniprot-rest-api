package org.uniprot.api.common.repository.search.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;
import org.uniprot.core.util.Utils;

class SuggestionConverterTest {

    private SuggestionConverter converter;
    private QueryResponse mockQueryResponse;
    private SpellCheckResponse mockSpellCheckResponse;

    private SolrDocumentList mockSolrDocList;

    @BeforeEach
    void setUp() {
        converter = new SuggestionConverter();
        mockQueryResponse = mock(QueryResponse.class);
        mockSpellCheckResponse = mock(SpellCheckResponse.class);
        mockSolrDocList = mock(SolrDocumentList.class);
        when(mockQueryResponse.getResults()).thenReturn(mockSolrDocList);
        when(mockSolrDocList.getNumFound()).thenReturn(0L);
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
        String correctQuery = "bill";
        long hits = 1L;
        simulateSuggestionsReturnedAre(List.of(new PairImpl<>(correctQuery, hits)));

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).getQuery(), is(correctQuery));
        assertThat(suggestions.get(0).getHits(), is(hits));
    }

    @Test
    void convertsMultipleSolrSuggestionsToMultipleUniProtSuggestions() {
        // given --------------------------------------------------------------
        String correctQuery0 = "bill";
        long hit0 = 8L;

        String correctQuery1 = "ship";
        long hit1 = 2L;
        List<Pair<String, Long>> termHitPairs =
                List.of(new PairImpl<>(correctQuery0, hit0), new PairImpl<>(correctQuery1, hit1));
        simulateSuggestionsReturnedAre(termHitPairs);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(2));
        assertThat(suggestions.get(0).getQuery(), is(correctQuery0));
        assertThat(suggestions.get(0).getHits(), is(hit0));

        assertThat(suggestions.get(1).getQuery(), is(correctQuery1));
        assertThat(suggestions.get(1).getHits(), is(hit1));
    }

    @Test
    void returnsBasedOnTheSpellCheckSuggestionsWhenCollationsAreNotPresent() {
        // given --------------------------------------------------------------
        String correctQuery0 = "bill";
        int hit0 = 8;
        String correctQuery1 = "will";
        int hit1 = 2;
        Map<String, List<Pair<String, Integer>>> termHitPairs =
                Map.of(
                        "cill",
                        List.of(
                                new PairImpl<>(correctQuery0, hit0),
                                new PairImpl<>(correctQuery1, hit1)));
        simulateSpellCheckSuggestionsReturnedAre(termHitPairs);
        NamedList values = new NamedList(Map.of("params", new NamedList(Map.of("q", "cill"))));
        when(mockQueryResponse.getResponseHeader()).thenReturn(values);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(2));
        assertThat(suggestions.get(0).getQuery(), is(correctQuery0));
        assertThat(suggestions.get(0).getHits(), is((long) hit0));

        assertThat(suggestions.get(1).getQuery(), is(correctQuery1));
        assertThat(suggestions.get(1).getHits(), is((long) hit1));
    }

    @Test
    void returnsBasedOnTheSpellCheckSuggestionsWhenCollationsArePresent() {
        // given --------------------------------------------------------------
        String correctQuery0 = "bill";
        int hit0 = 8;
        String correctQuery1 = "will";
        int hit1 = 2;
        Map<String, List<Pair<String, Integer>>> termHitPairs =
                Map.of(
                        "cill",
                        List.of(
                                new PairImpl<>(correctQuery0, hit0),
                                new PairImpl<>(correctQuery1, hit1)));
        simulateSpellCheckSuggestionsReturnedAre(termHitPairs);

        String correctCollatedQuery0 = "mill";
        long hitCollated0 = 8L;
        String correctCollatedQuery1 = "gill";
        long hitCollated1 = 2L;
        List<Pair<String, Long>> termHitCollatedPairs =
                List.of(
                        new PairImpl<>(correctCollatedQuery0, hitCollated0),
                        new PairImpl<>(correctCollatedQuery1, hitCollated1));
        simulateSuggestionsReturnedAre(termHitCollatedPairs);
        NamedList values = new NamedList(Map.of("params", new NamedList(Map.of("q", "cill"))));
        when(mockQueryResponse.getResponseHeader()).thenReturn(values);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(2));
        assertThat(suggestions.get(0).getQuery(), is(correctCollatedQuery0));
        assertThat(suggestions.get(0).getHits(), is(hitCollated0));

        assertThat(suggestions.get(1).getQuery(), is(correctCollatedQuery1));
        assertThat(suggestions.get(1).getHits(), is(hitCollated1));
    }

    @Test
    void
            returnsBasedOnTheSpellCheckSuggestionsWhenCollationsAreNotPresentAndQueryValueHasTwoWords() {
        // given --------------------------------------------------------------
        String correctQuery0 = "bill";
        int hit0 = 8;
        String correctQuery1 = "dill";
        int hit1 = 2;
        Map<String, List<Pair<String, Integer>>> termHitPairs =
                Map.of(
                        "cill",
                        List.of(
                                new PairImpl<>(correctQuery0, hit0),
                                new PairImpl<>(correctQuery1, hit1)));
        simulateSpellCheckSuggestionsReturnedAre(termHitPairs);
        NamedList values =
                new NamedList(Map.of("params", new NamedList(Map.of("q", "cill Gates"))));
        when(mockQueryResponse.getResponseHeader()).thenReturn(values);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(2));
        assertThat(suggestions.get(0).getQuery(), is(correctQuery0 + " Gates"));
        assertThat(suggestions.get(0).getHits(), is((long) hit0));

        assertThat(suggestions.get(1).getQuery(), is(correctQuery1 + " Gates"));
        assertThat(suggestions.get(1).getHits(), is((long) hit1));
    }

    @Test
    void noSuggestionsReturnedWhenResultFound() {
        // given --------------------------------------------------------------
        String correctQuery = "bill";
        long resultHist = 5L;
        long suggestionHits = 1L;
        when(mockSolrDocList.getNumFound()).thenReturn(resultHist);
        simulateSuggestionsReturnedAre(List.of(new PairImpl<>(correctQuery, suggestionHits)));
        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(mockQueryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(0));
    }

    @SuppressWarnings("unchecked")
    private void simulateSuggestionsReturnedAre(List<Pair<String, Long>> queryHitsPairs) {
        if (Utils.notNullNotEmpty(queryHitsPairs)) {
            List<SpellCheckResponse.Collation> mockCollations = new ArrayList<>();

            for (Pair<String, Long> termHit : queryHitsPairs) {

                String query = termHit.getKey();
                Long hits = termHit.getValue();
                SpellCheckResponse.Collation mockCollation =
                        mock(SpellCheckResponse.Collation.class);
                when(mockCollation.getCollationQueryString()).thenReturn(query);
                when(mockCollation.getNumberOfHits()).thenReturn(hits);
                mockCollations.add(mockCollation);
            }
            when(mockSpellCheckResponse.getCollatedResults()).thenReturn(mockCollations);
        } else {
            when(mockQueryResponse.getSpellCheckResponse()).thenReturn(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void simulateSpellCheckSuggestionsReturnedAre(
            Map<String, List<Pair<String, Integer>>> queryHitsPairs) {
        if (Utils.notNullNotEmpty(queryHitsPairs)) {
            List<SpellCheckResponse.Suggestion> mockSuggestions = mock(List.class);
            when(mockSuggestions.isEmpty()).thenReturn(false);
            when(mockSuggestions.size()).thenReturn(queryHitsPairs.size());

            Map<String, SpellCheckResponse.Suggestion> suggestionMap = new HashMap<>();
            for (String key : queryHitsPairs.keySet()) {
                SpellCheckResponse.Suggestion mockSuggestion =
                        mock(SpellCheckResponse.Suggestion.class);
                when(mockSuggestion.getNumFound()).thenReturn(queryHitsPairs.get(key).size());
                when(mockSuggestion.getAlternatives())
                        .thenReturn(queryHitsPairs.get(key).stream().map(Pair::getKey).toList());
                when(mockSuggestion.getAlternativeFrequencies())
                        .thenReturn(queryHitsPairs.get(key).stream().map(Pair::getValue).toList());
                suggestionMap.put(key, mockSuggestion);
            }

            when(mockSpellCheckResponse.getSuggestions()).thenReturn(mockSuggestions);
            when(mockSpellCheckResponse.getSuggestionMap()).thenReturn(suggestionMap);
            when(mockQueryResponse.getSpellCheckResponse()).thenReturn(mockSpellCheckResponse);
        } else {
            when(mockQueryResponse.getSpellCheckResponse()).thenReturn(null);
        }
    }
}
