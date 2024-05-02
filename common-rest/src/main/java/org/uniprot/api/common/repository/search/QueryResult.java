package org.uniprot.api.common.repository.search;

import java.util.Collection;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.term.TermInfo;

import lombok.Builder;
import lombok.Getter;

/**
 * Solr Repository response entity
 *
 * @author lgonzales
 */
@Getter
@Builder
public class QueryResult<T> {
    private final Collection<TermInfo> matchedFields;
    private final Collection<Suggestion> suggestions;
    private Page page;
    private final Stream<T> content;
    private final Collection<Facet> facets;
    private final ExtraOptions extraOptions;
    private final Collection<ProblemPair> warnings;

    public Page getPageAndClean() {
        Page result = this.page;
        this.page = null;
        return result;
    }
}
