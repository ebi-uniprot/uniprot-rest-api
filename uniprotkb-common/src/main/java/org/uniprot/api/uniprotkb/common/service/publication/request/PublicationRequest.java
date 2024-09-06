package org.uniprot.api.uniprotkb.common.service.publication.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.request.SearchRequest.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniprotkb.common.service.publication.PublicationFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
@ParameterObject
public class PublicationRequest {

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
    @PositiveOrZero(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = FACET_FILTER_PUBLICATION, example = "types:1")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = PublicationFacetConfig.class)
    private String facetFilter;

    @Parameter(description = FACETS_PUBLICATION, example = "types,categories")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    @ValidFacets(facetConfig = PublicationFacetConfig.class)
    private String facets;
}
