package org.uniprot.api.idmapping.controller.request;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Created 25/02/2021
 *
 * @author Edd
 */
@Data
public class IdMappingPageRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Pagination size. Defaults to 25.")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
