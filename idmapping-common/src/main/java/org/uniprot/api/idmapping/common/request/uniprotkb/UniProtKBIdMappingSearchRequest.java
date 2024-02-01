package org.uniprot.api.idmapping.common.request.uniprotkb;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.validation.ValidFacets;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniProtKBIdMappingSearchRequest extends UniProtKBIdMappingBasicRequest
        implements SearchRequest {

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniprotkb-return-fields.json")
    @Parameter(description = "Name of the facet search")
    @ValidFacets(facetConfig = UniProtKBFacetConfig.class)
    private String facets;

    @Override
    public void removeFacets() {
        this.facets = null;
    }
}
