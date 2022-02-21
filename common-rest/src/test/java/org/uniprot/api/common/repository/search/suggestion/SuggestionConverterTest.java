package org.uniprot.api.common.repository.search.suggestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;
import org.uniprot.core.util.Utils;

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
}
