package org.uniprot.api.aa.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;

import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Data
public class ArbaBasicRequest {

    @Parameter(description = QUERY_ARBA_DESCRIPTION, example = QUERY_ARBA_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.ARBA, messagePrefix = "search.arba")
    private String query;

    @Parameter(description = SORT_ARBA_DESCRIPTION, example = SORT_ARBA_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.ARBA)
    private String sort;

    @Parameter(description = FIELDS_ARBA_DESCRIPTION, example = FIELDS_ARBA_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.ARBA)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
