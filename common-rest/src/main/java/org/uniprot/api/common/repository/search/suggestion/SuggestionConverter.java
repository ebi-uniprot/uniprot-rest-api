package org.uniprot.api.common.repository.search.suggestion;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
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
        if (resultHits == 0L
                && Utils.notNull(spellCheckResponse)
                && Utils.notNullNotEmpty(spellCheckResponse.getCollatedResults())) {
            return spellCheckResponse.getCollatedResults().stream()
                    .map(this::getSuggestion)
                    .collect(Collectors.toList());
        } else {
            return emptyList();
        }
    }

    private Suggestion getSuggestion(SpellCheckResponse.Collation collation) {
        return Suggestion.builder()
                .query(collation.getCollationQueryString().replace(" AND ", " "))
                .hits(collation.getNumberOfHits())
                .build();
    }
}
