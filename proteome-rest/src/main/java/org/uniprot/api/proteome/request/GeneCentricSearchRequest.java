package org.uniprot.api.proteome.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author jluo
 * @date: 17 May 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class GeneCentricSearchRequest extends GeneCentricBasicRequest implements SearchRequest {

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = GeneCentricFacetConfig.class)
    private String facets;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
