package org.uniprot.api.uniprotkb.controller.request;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniprotkb.service.PublicationFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
@ParameterObject
public class PublicationRequest {

    @Parameter(description = "Pagination size. Defaults to 25.")
    @PositiveOrZero(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Facet filter query for Publications")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = PublicationFacetConfig.class)
    private String facetFilter;

    @Parameter(description = "Name of the facet search")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    @ValidFacets(facetConfig = PublicationFacetConfig.class)
    private String facets;
}
