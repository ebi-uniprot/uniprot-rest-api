package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceFacetConfig;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author sahmad
 * @created 29/03/2021
 */
@Data
@ParameterObject
public class UniParcDatabasesRequest extends UniParcGetByIdRequest implements SearchRequest {

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC_CROSSREF)
    private String fields;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = SIZE_DESCRIPTION, example = SIZE_EXAMPLE)
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = SearchRequest.MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    @ValidFacets(facetConfig = UniParcCrossReferenceFacetConfig.class)
    private String facets;

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }
}
