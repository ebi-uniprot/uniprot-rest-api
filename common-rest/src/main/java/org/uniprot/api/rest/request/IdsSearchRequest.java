package org.uniprot.api.rest.request;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author sahmad
 * @created 18/03/2021 An interface to search entries by comma separated unique ids
 */
public interface IdsSearchRequest extends SearchRequest {
    String getCommaSeparatedIds();

    String getDownload();

    String getFacetFilter();

    default String getQuery() {
        return null;
    }

    default String getSort() {
        return null;
    }

    default boolean isDownload() {
        return Boolean.parseBoolean(getDownload());
    }

    default List<String> getIdList() {
        return List.of(getCommaSeparatedIds().split(",")).stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }
}
