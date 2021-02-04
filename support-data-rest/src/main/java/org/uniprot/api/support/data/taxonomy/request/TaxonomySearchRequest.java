package org.uniprot.api.support.data.taxonomy.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;

@Data
public class TaxonomySearchRequest extends TaxonomyBasicRequest implements SearchRequest {

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Comma separated list of facets to search")
    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    private String facets;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
