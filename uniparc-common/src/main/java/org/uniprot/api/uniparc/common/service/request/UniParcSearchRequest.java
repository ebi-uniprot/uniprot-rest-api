package org.uniprot.api.uniparc.common.service.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcSearchRequest extends UniParcBasicRequest implements SearchRequest {

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    protected String facets;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = SearchRequest.MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
