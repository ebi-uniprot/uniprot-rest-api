package org.uniprot.api.support.data.common.keyword.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.support.data.common.keyword.repository.KeywordFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

@Data
@EqualsAndHashCode(callSuper = true)
public class KeywordSearchRequest extends KeywordBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = "Comma separated list of facets to search")
    @ValidFacets(facetConfig = KeywordFacetConfig.class)
    private String facets;
}
