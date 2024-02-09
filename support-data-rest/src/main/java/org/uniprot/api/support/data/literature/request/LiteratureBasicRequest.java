package org.uniprot.api.support.data.literature.request;

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
 * @created 22/01/2021
 */
@Data
public class LiteratureBasicRequest {

    @Parameter(description = QUERY_LIT_DESCRIPTION, example = QUERY_LIT_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.LITERATURE,
            messagePrefix = "search.literature")
    private String query;

    @Parameter(description = SORT_LIT_DESCRIPTION, example = SORT_LIT_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.LITERATURE)
    private String sort;

    @Parameter(description = FIELDS_LIT_DESCRIPTION, example = FIELDS_LIT_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.LITERATURE)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
