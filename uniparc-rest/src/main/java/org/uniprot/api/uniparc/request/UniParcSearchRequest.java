package org.uniprot.api.uniparc.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
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
@ParameterObject
public class UniParcSearchRequest extends UniParcBasicRequest implements SearchRequest {

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    protected String facets;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
