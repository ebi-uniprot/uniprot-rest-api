package org.uniprot.api.uniprotkb.controller.request;

import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.uniprotkb.service.PublicationFacetConfig;

/**
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
public class PublicationRequest {

    @Positive(message = "{search.positive}")
    private Integer size = 25;

    private String cursor;

    // TODO: add query validation...
    private String query;

    @ValidFacets(facetConfig = PublicationFacetConfig.class)
    private String facets;
}
