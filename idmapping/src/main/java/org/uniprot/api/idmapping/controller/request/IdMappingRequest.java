package org.uniprot.api.idmapping.controller.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import io.swagger.v3.oas.annotations.Parameter;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

/**
 * @author sahmad
 * @created 22/02/2021
 */
//FIXME - probably we can delete this class
@Data
@Builder
@AllArgsConstructor
public class IdMappingRequest {
    @NotNull(message = "{search.required}")
    @Parameter(description = "Name of the from type")
    private String from; // TODO add a from validator to verify supported from

    @NotNull(message = "{search.required}")
    @Parameter(description = "Name of the to type")
    private String to; // TODO add a to validator based on from

    @NotNull(message = "{search.required}")
    @Parameter(description = "Comma separated list of ids")
    private String ids; // TODO add validation like length, regex

    @Parameter(description = "Value of the taxon Id")
    private String taxId;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
