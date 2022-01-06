package org.uniprot.api.idmapping.controller.request.uniref;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.validation.ValidFacets;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefIdMappingSearchRequest extends UniRefIdMappingBasicRequest
        implements SearchRequest {

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniref-return-fields.json")
    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Override
    public void removeFacets() {
        this.facets = null;
    }
}
