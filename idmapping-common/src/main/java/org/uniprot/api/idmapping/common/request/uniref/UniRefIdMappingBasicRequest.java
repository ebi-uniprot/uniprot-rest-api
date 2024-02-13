package org.uniprot.api.idmapping.common.request.uniref;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.idmapping.controller.request.IdMappingPageRequest;
import org.uniprot.api.idmapping.common.request.IdMappingPageRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 25/02/2021
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniRefIdMappingBasicRequest extends IdMappingPageRequest {

    @Parameter(description = QUERY_UNIREF_DESCRIPTION, example = QUERY_UNIREF_EXAMPLE)
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.UNIREF, messagePrefix = "search.uniref")
    private String query;

    @Parameter(description = FIELDS_UNIREF_DESCRIPTION, example = FIELDS_UNIREF_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    private String fields;

    @Parameter(description = SORT_UNIREF_DESCRIPTION, example = SORT_UNIREF_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIREF)
    private String sort;

    @Parameter(hidden = true)
    private String format;
}
