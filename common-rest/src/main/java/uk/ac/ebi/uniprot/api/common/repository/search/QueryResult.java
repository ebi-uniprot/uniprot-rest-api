package uk.ac.ebi.uniprot.api.common.repository.search;

import uk.ac.ebi.uniprot.api.common.repository.search.facet.Facet;
import uk.ac.ebi.uniprot.api.common.repository.search.page.Page;

import java.util.Collection;

/**
 * Solr Repository response entity
 *
 * @author lgonzales
 */
public class QueryResult<T> {

    private Page page;
    private final Collection<T> content;
    private final Collection<Facet> facets;

    private QueryResult(Collection<T> content, Page page, Collection<Facet> facets) {
        this.content = content;
        this.page = page;
        this.facets = facets;
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page) {
        return new QueryResult<>(content, page, null);
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page, Collection<Facet> facets) {
        return new QueryResult<>(content, page, facets);
    }

    public Page getPage() {
        return page;
    }

    public Page getPageAndClean() {
        Page result = this.page;
        this.page = null;
        return result;
    }

    public Collection<T> getContent() {
        return content;
    }

    public Collection<Facet> getFacets() {
        return facets;
    }
}
