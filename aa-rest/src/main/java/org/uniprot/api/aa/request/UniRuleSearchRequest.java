package org.uniprot.api.aa.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.aa.repository.UniRuleFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;

@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniRuleSearchRequest extends UniRuleBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniRuleFacetConfig.class)
    private String facets;

    @Parameter(description = "Pagination size. Defaults to 25.")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
