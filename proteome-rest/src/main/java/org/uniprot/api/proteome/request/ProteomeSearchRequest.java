package org.uniprot.api.proteome.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
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
@ParameterObject
public class ProteomeSearchRequest extends ProteomeBasicRequest implements SearchRequest {

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = ProteomeFacetConfig.class)
    private String facets;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
