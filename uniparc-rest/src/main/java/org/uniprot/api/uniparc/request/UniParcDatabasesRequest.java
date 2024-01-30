package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 29/03/2021
 */
@Data
public class UniParcDatabasesRequest extends UniParcGetByIdRequest implements SearchRequest {

    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC_CROSSREF)
    private String fields;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }

    @Override
    public String getFacets() {
        return null;
    }
}
