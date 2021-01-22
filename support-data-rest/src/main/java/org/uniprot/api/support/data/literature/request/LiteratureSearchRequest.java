package org.uniprot.api.support.data.literature.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.support.data.literature.repository.LiteratureFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Data
public class LiteratureSearchRequest extends LiteratureBasicRequest implements SearchRequest {
    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(description = "Comma separated list of facets to search")
    @ValidFacets(facetConfig = LiteratureFacetConfig.class)
    private String facets;
}
