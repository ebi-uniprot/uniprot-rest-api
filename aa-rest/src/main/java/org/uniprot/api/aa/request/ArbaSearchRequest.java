package org.uniprot.api.aa.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.aa.repository.ArbaFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class ArbaSearchRequest extends ArbaBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = ArbaFacetConfig.class)
    private String facets;

    @Parameter(description = "Pagination size. Defaults to 25.")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
