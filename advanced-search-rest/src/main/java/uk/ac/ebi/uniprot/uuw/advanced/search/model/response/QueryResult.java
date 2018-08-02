package uk.ac.ebi.uniprot.uuw.advanced.search.model.response;


import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.Page;

import java.util.Collection;

/**
 * Solr Repository response entity
 *
 * @author lgonzales
 */
public class QueryResult<T> {

    private final Page page;
    private final Collection<T> content;

    private  QueryResult(Collection<T> content, Page page) {
        this.content = content;
        this.page = page;
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page){
        return new QueryResult<>(content,page);
    }

    public Page getPage() {
        return page;
    }

    public Collection<T> getContent() {
        return content;
    }
}
