package org.uniprot.api.uniref.common.service.member.request;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniref.common.repository.store.UniRefEntryFacetConfig;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 05/01/2021
 */
@Data
public class UniRefMemberRequest {

    @Parameter(description = "Unique identifier for the UniRef cluster")
    @Pattern(
            regexp = FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.id.value}")
    @NotNull(message = "{search.required}")
    private String id;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniRefEntryFacetConfig.class)
    private String facets;

    @Parameter(description = "Facet filter query for UniRef Cluster Members")
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
