package org.uniprot.api.uniparc.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class UniParcGetByIdPageSearchRequest extends UniParcGetByIdRequest
        implements SearchRequest {

    @Parameter(description = "Comma separated list of fields to be returned in the response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @Parameter(description = OpenApiConstants.SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Override
    public String getSort() {
        return null;
    }

    @Override
    public String getFacets() {
        return null;
    }
}
