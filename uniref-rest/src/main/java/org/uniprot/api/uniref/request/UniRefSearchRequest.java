package org.uniprot.api.uniref.request;

import javax.validation.constraints.Positive;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefSearchRequest extends UniRefBasicRequest implements SearchRequest {

    @Parameter(hidden = true)
    public static final String DEFAULT_FIELDS = "id,name,common_taxon,count,created";

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;
}
