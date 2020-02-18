package org.uniprot.api.rest.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.data.solr.core.query.SimpleQuery;
import org.uniprot.core.util.Utils;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
public interface SearchRequest {

    Integer DEFAULT_RESULTS_SIZE = 25;

    String getQuery();

    String getFields();

    String getSort();

    String getCursor();

    String getFacets();

    Integer getSize();

    void setSize(Integer size);

    default boolean hasFields() {
        return Utils.notNullNotEmpty(getFields());
    }

    default boolean hasSort() {
        return Utils.notNullNotEmpty(getSort());
    }

    default boolean hasCursor() {
        return Utils.notNullNotEmpty(getCursor());
    }

    default boolean hasFacets() {
        return Utils.notNullNotEmpty(getFacets());
    }

    default SimpleQuery getSimpleQuery() {
        return new SimpleQuery(getQuery());
    }

    default List<String> getFacetList() {
        if (hasFacets()) {
            return Arrays.asList(getFacets().split(("\\s*,\\s*")));
        } else {
            return Collections.emptyList();
        }
    }
}
