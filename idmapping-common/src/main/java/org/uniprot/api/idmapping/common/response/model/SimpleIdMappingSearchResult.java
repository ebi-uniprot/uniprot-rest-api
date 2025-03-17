package org.uniprot.api.idmapping.common.response.model;

import org.uniprot.api.rest.openapi.IdMappingSearchResult;

import lombok.Getter;

@Getter
public class SimpleIdMappingSearchResult extends IdMappingSearchResult<String> {
    SimpleIdMappingSearchResult() {
        super();
    }
}
