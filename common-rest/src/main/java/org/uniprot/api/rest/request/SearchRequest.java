package org.uniprot.api.rest.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.uniprot.core.util.Utils;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
public interface SearchRequest extends BasicRequest {

    int MAX_RESULTS_SIZE = 500;

    String getFacets();

    String getCursor();

    Integer getSize();

    void setSize(Integer size);

    default boolean hasCursor() {
        return Utils.notNullNotEmpty(getCursor());
    }

    default boolean hasFacets() {
        return Utils.notNullNotEmpty(getFacets());
    }

    default List<String> getFacetList() {
        if (hasFacets()) {
            return Arrays.asList(getFacets().split(("\\s*,\\s*")));
        } else {
            return Collections.emptyList();
        }
    }

    default void removeFacets() {
        // empty method to avoid overriding in other subclasses
    }
}
