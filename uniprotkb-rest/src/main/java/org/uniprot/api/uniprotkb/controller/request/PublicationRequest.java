package org.uniprot.api.uniprotkb.controller.request;

import static org.uniprot.api.rest.request.SearchRequest.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniprotkb.service.PublicationFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * /search?query=????
 * /accession/P12345/publications?query=????
 *
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
public class PublicationRequest {

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Facet filter query for Publications")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = PublicationFacetConfig.class)
    private String query;

    @Parameter(description = "Name of the facet search")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    @ValidFacets(facetConfig = PublicationFacetConfig.class)
    private String facets;
}
