package org.uniprot.api.support.data.disease.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;

import io.swagger.v3.oas.annotations.Parameter;

@Data
public class DiseaseSearchRequest extends DiseaseBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    @Override
    public String getFacets() {
        return "";
    }
}
