package org.uniprot.api.aa.request;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Data
public class ArbaBasicRequest {

    @Parameter(description = QUERY_DESCRIPTION)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.ARBA, messagePrefix = "search.arba")
    private String query;

    @Parameter(description = SORT_DESCRIPTION)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.ARBA)
    private String sort;

    @Parameter(description = FIELDS_DESCRIPTION)
    @ValidReturnFields(uniProtDataType = UniProtDataType.ARBA)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
