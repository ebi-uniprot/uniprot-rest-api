package org.uniprot.api.support.data.subcellular.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;

import io.swagger.v3.oas.annotations.Parameter;

@Data
public class SubcellularLocationSearchRequest extends SubcellularLocationBasicRequest
        implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    @Override
    public String getFacets() {
        return "";
    }
}
