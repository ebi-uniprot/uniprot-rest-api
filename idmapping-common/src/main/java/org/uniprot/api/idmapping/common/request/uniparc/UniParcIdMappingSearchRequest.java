package org.uniprot.api.idmapping.common.request.uniparc;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniParcIdMappingSearchRequest extends UniParcIdMappingBasicRequest
        implements SearchRequest {

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    private String facets;

    @Override
    public void removeFacets() {
        this.facets = null;
    }
}
