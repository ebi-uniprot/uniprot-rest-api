package org.uniprot.api.rest.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.SIZE_DESCRIPTION;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.SIZE_EXAMPLE;
import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

@Data
public class PageRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
