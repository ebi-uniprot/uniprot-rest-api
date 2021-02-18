package org.uniprot.api.idmapping.controller.request;

import lombok.*;

import org.uniprot.api.rest.request.SearchRequest;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdMappingSearchRequest extends IdMappingBasicRequest implements SearchRequest {
    private String facets;

    private String facetFilter;

    @Override
    public String getQuery() {
        return "";
    }
}
