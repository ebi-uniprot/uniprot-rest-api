package org.uniprot.api.support.data.suggester.service;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.support.data.suggester.response.Suggestion;
import org.uniprot.api.support.data.suggester.response.Suggestions;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.suggest.SuggestDictionary;
import org.uniprot.store.search.document.suggest.SuggestDocument;
import org.uniprot.store.search.field.QueryBuilder;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
@Slf4j
public class SuggesterService {
    private static final String SUGGEST_SEARCH_HANDLER = "/search";
    private static final String ERROR_FORMAT;

    static {
        String dicts =
                Stream.of(SuggestDictionary.values())
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.joining(", ", "[", "]"));
        ERROR_FORMAT = "Unknown dictionary: '%s'. Expected one of " + dicts + ".";
    }

    private final SolrClient solrClient;
    private final SolrCollection collection;
    private final SearchFieldConfig searchFieldConfig;

    public SuggesterService(SolrClient solrClient, SolrCollection collection) {
        this.solrClient = solrClient;
        this.collection = collection;
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.SUGGEST);
    }

    public Suggestions findSuggestions(String dictionaryStr, String queryStr) {
        SolrQuery solrQuery =
                new SolrQuery(createQueryString(getDictionary(dictionaryStr), queryStr));
        solrQuery.setRequestHandler(SUGGEST_SEARCH_HANDLER);

        try {
            List<SuggestDocument> content =
                    solrClient.query(collection.name(), solrQuery).getBeans(SuggestDocument.class);
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
            throw new InvalidRequestException(String.format(ERROR_FORMAT, dictionaryStr));
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
                        searchFieldConfig.getSearchFieldItemByName("dict").getFieldName(),
                        dict.name());
    }
}
