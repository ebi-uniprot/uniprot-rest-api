package org.uniprot.api.idmapping.common.request.uniprotkb;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.ValidFacets;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ParameterObject
public class UniProtKBIdMappingSearchRequest extends UniProtKBIdMappingBasicRequest
        implements SearchRequest {

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniProtKBFacetConfig.class)
    private String facets;

    @Override
    public void removeFacets() {
        this.facets = null;
    }
}
