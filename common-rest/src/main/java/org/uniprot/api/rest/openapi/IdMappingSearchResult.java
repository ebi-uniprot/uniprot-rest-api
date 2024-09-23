package org.uniprot.api.rest.openapi;

import java.util.List;

import lombok.Getter;

@Getter
public class IdMappingSearchResult<T> extends SearchResult<T> {
    IdMappingSearchResult() {
        super();
    }

    Integer obsoleteCount;
    List<String> suggestedIds;
    List<String> failedIds;
}
