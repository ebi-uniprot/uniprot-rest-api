package org.uniprot.api.uniref.common.service.light.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.validation.*;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniRefSearchRequest extends UniRefBasicRequest implements SearchRequest {

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
