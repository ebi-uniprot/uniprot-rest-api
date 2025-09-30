package org.uniprot.api.common.repository.search.suggestion;

import static java.util.Collections.emptyList;
import static org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.common.util.NamedList;
import org.springframework.core.convert.converter.Converter;
import org.uniprot.core.util.Utils;

/**
 * Converts a {@link QueryResponse}'s spellcheck component to a list of {@link Suggestion}s. Created
 *
 * <p>29/07/21
 *
 * @author Edd
 */
public class SuggestionConverter implements Converter<QueryResponse, List<Suggestion>> {

    @Override
    public List<Suggestion> convert(QueryResponse queryResponse) {
        long resultHits = queryResponse.getResults().getNumFound();
        SpellCheckResponse spellCheckResponse = queryResponse.getSpellCheckResponse();
        if (resultHits == 0L && Utils.notNull(spellCheckResponse)) {
            // Case 1: Solr already returned collations
            if (Utils.notNullNotEmpty(spellCheckResponse.getCollatedResults())) {
                return spellCheckResponse.getCollatedResults().stream()
                        .map(this::getSuggestion)
                        .toList();
            }

            // Case 2: Build client-side collations from token suggestions
            if (Utils.notNullNotEmpty(spellCheckResponse.getSuggestions())) {
                String query =
                        (String) ((NamedList<?>) queryResponse.getHeader().get("params")).get("q");
                if (!StringUtils.containsWhitespace(query)) {
                    return spellCheckResponse.getSuggestions().stream()
                            .flatMap(
                                    sug ->
                                            sug.getAlternatives().stream()
                                                    .map(alt -> getCollation(sug, alt, query)))
                            .map(this::getSuggestion)
                            .toList();
                }
            }
        }

        return emptyList();
    }

    private static Collation getCollation(
            SpellCheckResponse.Suggestion suggestion, String alternative, String query) {
        String collationQuery = query.toLowerCase().replace(suggestion.getToken(), alternative);
        return new Collation().setCollationQueryString(collationQuery);
    }

    private Suggestion getSuggestion(Collation collation) {
        return Suggestion.builder()
                .query(collation.getCollationQueryString().replace(" AND ", " "))
                .hits(collation.getNumberOfHits())
                .build();
    }
}
