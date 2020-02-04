package org.uniprot.api.suggester.service;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.suggester.Suggestion;
import org.uniprot.api.suggester.Suggestions;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.suggest.SuggestDictionary;
import org.uniprot.store.search.document.suggest.SuggestDocument;
import org.uniprot.store.search.field.QueryBuilder;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
@Slf4j
public class SuggesterService {
    private static final String SUGGEST_SEARCH_HANDLER = "/search";
    private static String errorFormat;

    static {
        String dicts =
                Stream.of(SuggestDictionary.values())
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.joining(", ", "[", "]"));
        errorFormat = "Unknown dictionary: '%s'. Expected one of " + dicts + ".";
    }

    private final SolrTemplate solrTemplate;
    private final SolrCollection collection;

    public SuggesterService(SolrTemplate solrTemplate, SolrCollection collection) {
        this.solrTemplate = solrTemplate;
        this.collection = collection;
    }

    public Suggestions findSuggestions(String dictionaryStr, String queryStr) {
        SimpleQuery query =
                new SimpleQuery(createQueryString(getDictionary(dictionaryStr), queryStr));
        query.setRequestHandler(SUGGEST_SEARCH_HANDLER);

        try {
            List<SuggestDocument> content =
                    solrTemplate
                            .query(collection.name(), query, SuggestDocument.class)
                            .getContent();
            return Suggestions.builder()
                    .dictionary(dictionaryStr)
                    .query(queryStr)
                    .suggestions(convertDocs(content))
                    .build();
        } catch (Exception e) {
            log.error("Problem when retrieving suggestions", e);
            throw new ServiceException(
                    "An internal server error occurred when retrieving suggestions.", e);
        }
    }

    SuggestDictionary getDictionary(String dictionaryStr) {
        try {
            return SuggestDictionary.valueOf(dictionaryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(String.format(errorFormat, dictionaryStr));
        }
    }

    List<Suggestion> convertDocs(List<SuggestDocument> content) {
        return content.stream()
                .map(
                        doc -> {
                            String value = doc.value;
                            if (Utils.notNullNotEmpty(doc.altValues) && !doc.altValues.isEmpty()) {
                                StringJoiner joiner = new StringJoiner("/", " (", ")");
                                doc.altValues.forEach(joiner::add);
                                value += joiner.toString();
                            }
                            return Suggestion.builder().id(doc.id).value(value).build();
                        })
                .collect(Collectors.toList());
    }

    private String createQueryString(SuggestDictionary dict, String query) {
        return "\""
                + query
                + "\""
                + " +"
                + QueryBuilder.query(
                        UniProtSearchFields.SUGGEST.getField("dict").getName(), dict.name());
    }
}
