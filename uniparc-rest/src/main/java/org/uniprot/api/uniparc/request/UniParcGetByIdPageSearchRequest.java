package org.uniprot.api.uniparc.request;

import org.uniprot.api.rest.request.SearchRequest;

import javax.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class UniParcGetByIdPageSearchRequest extends UniParcGetByIdRequest implements SearchRequest {
    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Override
    public String getFacets() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }
}
