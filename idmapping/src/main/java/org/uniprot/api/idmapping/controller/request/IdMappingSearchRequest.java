package org.uniprot.api.idmapping.controller.request;

import org.uniprot.api.rest.request.SearchRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IdMappingSearchRequest extends IdMappingBasicRequest implements SearchRequest {
    private String facets;

    private String cursor;

    private Integer size;

    @Override
    public String getQuery() {
        return "";
    }
}
