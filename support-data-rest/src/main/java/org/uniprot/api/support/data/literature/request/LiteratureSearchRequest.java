package org.uniprot.api.support.data.literature.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.support.data.literature.repository.LiteratureFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Data
@ParameterObject
public class LiteratureSearchRequest extends LiteratureBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = LiteratureFacetConfig.class)
    private String facets;
}
