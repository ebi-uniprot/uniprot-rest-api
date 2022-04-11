package org.uniprot.api.proteome.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProteomeSearchRequest extends ProteomeBasicRequest implements SearchRequest {

    @Parameter(hidden = true, description = "Name of the facet search")
    @ValidFacets(facetConfig = ProteomeFacetConfig.class)
    private String facets;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
