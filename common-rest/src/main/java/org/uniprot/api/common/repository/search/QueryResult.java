package org.uniprot.api.common.repository.search;

import java.util.Collection;
import java.util.stream.Stream;

import lombok.Getter;

import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.term.TermInfo;

/**
 * Solr Repository response entity
 *
 * @author lgonzales
 */
@Getter
public class QueryResult<T> {
    private final Collection<TermInfo> matchedFields;
    private final Collection<Suggestion> suggestions;
    private Page page;
    private final Stream<T> content;
    private final Collection<Facet> facets;
    private final Collection<String> failedIds;
    private final Collection<ProblemPair> warnings;

    private QueryResult(
            Stream<T> content,
            Page page,
            Collection<Facet> facets,
            Collection<TermInfo> matchedFields,
            Collection<Suggestion> suggestions,
            Collection<String> failedIds,
            Collection<ProblemPair> warnings) {
        this.content = content;
        this.page = page;
        this.facets = facets;
        this.matchedFields = matchedFields;
        this.suggestions = suggestions;
        this.failedIds = failedIds;
        this.warnings = warnings;
    }

    public static <T> QueryResult<T> of(Stream<T> content, Page page) {
        return new QueryResult<>(content, page, null, null, null, null, null);
    }

    public static <T> QueryResult<T> of(Stream<T> content, Page page, Collection<Facet> facets) {
        return new QueryResult<>(content, page, facets, null, null, null, null);
    }

    public static <T> QueryResult<T> of(
            Stream<T> content,
            Page page,
            Collection<Facet> facets,
            Collection<TermInfo> termInfos,
            Collection<String> failedIds,
            Collection<Suggestion> suggestions) {
        return new QueryResult<>(content, page, facets, termInfos, suggestions, failedIds, null);
    }

    public static <T> QueryResult<T> of(
            Stream<T> content,
            Page page,
            Collection<Facet> facets,
            Collection<String> failedIds,
            Collection<ProblemPair> warnings) {
        return new QueryResult<>(content, page, facets, null, null, failedIds, warnings);
    }

    public static <T> QueryResult<T> of(
            Stream<T> content,
            Page page,
            Collection<Facet> facets,
            Collection<TermInfo> termInfos,
            Collection<String> failedIds,
            Collection<Suggestion> suggestions,
            Collection<ProblemPair> warnings) {
        return new QueryResult<>(
                content, page, facets, termInfos, suggestions, failedIds, warnings);
    }

    public Page getPageAndClean() {
        Page result = this.page;
        this.page = null;
        return result;
    }
}
