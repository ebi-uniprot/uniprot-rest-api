package org.uniprot.api.common.repository.search.suggestion;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.springframework.core.convert.converter.Converter;
import org.uniprot.core.util.Utils;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

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
        SpellCheckResponse spellCheckResponse = queryResponse.getSpellCheckResponse();
        if (Utils.notNull(spellCheckResponse)) {
            return spellCheckResponse.getSuggestions().stream()
                    .map(
                            s -> {
                                Suggestion.SuggestionBuilder suggestionBuilder =
                                        Suggestion.builder().original(s.getToken());
                                for (int i = 0; i < s.getAlternatives().size(); i++) {
                                    suggestionBuilder.alternative(
                                            Alternative.builder()
                                                    .term(s.getAlternatives().get(i))
                                                    .count(s.getAlternativeFrequencies().get(i))
                                                    .build());
                                }
                                return suggestionBuilder.build();
                            })
                    .collect(Collectors.toList());
        } else {
            return emptyList();
        }
    }
}
