package org.uniprot.api.idmapping.common.request.uniref;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniRefIdMappingSearchRequest extends UniRefIdMappingBasicRequest
        implements SearchRequest {

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Override
    public void removeFacets() {
        this.facets = null;
    }
}
