package org.uniprot.api.idmapping.controller.request;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 24/02/2021
 */
@Data
public class IdMappingBasicSearchRequest {

    @NotNull(message = "{search.required}")
    private String jobId;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
