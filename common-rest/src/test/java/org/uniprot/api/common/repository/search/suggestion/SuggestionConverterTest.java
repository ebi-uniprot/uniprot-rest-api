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
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;

class SuggestionConverterTest {

    private SuggestionConverter converter;
    private QueryResponse mockQueryResponse;
    private SpellCheckResponse mockSpellCheckResponse;

    private SolrDocumentList mockSolrDocList;

    @BeforeEach
    void setUp() {
        converter = new SuggestionConverter(SolrCollection.uniprot);
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

    @Test
    void suggestionsForSingleWordQueryWhenAlternativesArePresentButNoCollations() {
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(0L);
        when(queryResponse.getResults()).thenReturn(results);

        SpellCheckResponse spellCheckResponse = mock(SpellCheckResponse.class);
        when(spellCheckResponse.getCollatedResults()).thenReturn(List.of());
        NamedList<Object> alternatives = new NamedList<>();
        alternatives.add("suggestion", List.of("correct"));
        SpellCheckResponse.Suggestion suggestion =
                new SpellCheckResponse.Suggestion("incorrect", alternatives);
        List<SpellCheckResponse.Suggestion> spellCheckSuggestions = List.of(suggestion);
        when(spellCheckResponse.getSuggestions()).thenReturn(spellCheckSuggestions);
        when(queryResponse.getSpellCheckResponse()).thenReturn(spellCheckResponse);
        NamedList<Object> headers = new NamedList<>();
        NamedList<Object> params = new NamedList<>();
        headers.add("params", params);
        params.add("q", "incorrect");
        when(queryResponse.getHeader()).thenReturn(headers);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = converter.convert(queryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).getQuery(), is("correct"));
    }

    @ParameterizedTest
    @CsvSource({
        "UniRef100_A0A001, (identity:1.0) AND (uniprotkb:A0A001)",
        "UniRef90_UPI003EBF2438, (identity:0.9) AND (uniparc:UPI003EBF2438)",
        "UniRef50_A0A009H2L0, (identity:0.5) AND (uniprotkb:A0A009H2L0)"
    })
    void suggestionsForUniRefIdSearch(String uniRefId, String suggestedQuery) {
        SuggestionConverter suggestionConverter = new SuggestionConverter(SolrCollection.uniref);
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(0L);
        when(queryResponse.getResults()).thenReturn(results);

        SpellCheckResponse spellCheckResponse = mock(SpellCheckResponse.class);
        when(queryResponse.getSpellCheckResponse()).thenReturn(spellCheckResponse);
        NamedList<Object> headers = new NamedList<>();
        NamedList<Object> params = new NamedList<>();
        headers.add("params", params);
        params.add("q", uniRefId);
        when(queryResponse.getHeader()).thenReturn(headers);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = suggestionConverter.convert(queryResponse);

        // then --------------------------------------------------------------
        assertThat(suggestions, hasSize(1));
        assertThat(suggestions.get(0).getQuery(), is(suggestedQuery));
    }

    @Test
    void suggestionsForInvalidUniRefIdSearch() {
        SuggestionConverter suggestionConverter = new SuggestionConverter(SolrCollection.uniref);
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(0L);
        when(queryResponse.getResults()).thenReturn(results);

        SpellCheckResponse spellCheckResponse = mock(SpellCheckResponse.class);
        when(queryResponse.getSpellCheckResponse()).thenReturn(spellCheckResponse);
        NamedList<Object> headers = new NamedList<>();
        NamedList<Object> params = new NamedList<>();
        headers.add("params", params);
        params.add("q", "UniRef100_PPPPP");
        when(queryResponse.getHeader()).thenReturn(headers);

        // when --------------------------------------------------------------
        List<Suggestion> suggestions = suggestionConverter.convert(queryResponse);

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
}
