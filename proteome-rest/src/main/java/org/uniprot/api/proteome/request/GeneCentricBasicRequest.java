package org.uniprot.api.proteome.request;

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
 * @author lgonzales
 * @since 29/10/2020
 */
@Data
public class GeneCentricBasicRequest {

    @Parameter(description = QUERY_DESCRIPTION)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.GENECENTRIC,
            messagePrefix = "search.genecentric")
    private String query;

    @Parameter(description = SORT_DESCRIPTION)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String sort;

    @Parameter(description = FIELDS_DESCRIPTION)
    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
