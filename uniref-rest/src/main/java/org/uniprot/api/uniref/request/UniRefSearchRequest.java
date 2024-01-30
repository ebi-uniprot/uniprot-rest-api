package org.uniprot.api.uniref.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.openapi.OpenApiConstants;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.validation.*;

import io.swagger.v3.oas.annotations.Parameter;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefSearchRequest extends UniRefBasicRequest implements SearchRequest {

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
