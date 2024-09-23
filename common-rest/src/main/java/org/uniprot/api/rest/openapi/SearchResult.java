package org.uniprot.api.rest.openapi;

import java.util.Collection;

import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.term.TermInfo;

import lombok.Getter;

/**
 * We had to create this class to document because spring-docs has a bug
 * with @ApiResponse @Content schemaProperties that ignore array items property
 * There is a bug for it that was fixed in release 2.3.0
 * https://github.com/springdoc/springdoc-openapi/pull/2403
 * Once we upgrade spring-doc we can remove it.
 * @param <T>
 */
@Getter
public class SearchResult<T> {
    SearchResult() {}

    Collection<T> results;
    Collection<Facet> facets;
    Collection<TermInfo> matchedFields;
    Collection<Suggestion> suggestions;
    Collection<ProblemPair> warnings;
}
