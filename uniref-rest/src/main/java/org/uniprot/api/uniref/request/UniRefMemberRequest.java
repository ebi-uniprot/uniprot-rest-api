package org.uniprot.api.uniref.request;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniref.repository.store.UniRefEntryFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 05/01/2021
 */
@Data
public class UniRefMemberRequest {
    @Parameter(hidden = true, description = "Name of the facet search")
    @ValidFacets(facetConfig = UniRefEntryFacetConfig.class)
    private String facets;

    @Parameter(hidden = true, description = "Facet filter query for UniRef Cluster Members")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = UniRefEntryFacetConfig.class)
    private String facetFilter;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
