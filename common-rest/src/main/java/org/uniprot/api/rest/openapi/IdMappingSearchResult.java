package org.uniprot.api.rest.openapi;

import lombok.Getter;

import java.util.List;

@Getter
public class IdMappingSearchResult<T> extends SearchResult<T> {
    IdMappingSearchResult(){
        super();
    }
    Integer obsoleteCount;
    List<String> suggestedIds;
    List<String> failedIds;
}