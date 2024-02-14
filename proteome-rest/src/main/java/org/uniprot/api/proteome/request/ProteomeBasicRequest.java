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
 * @since 23/11/2020
 */
@Data
public class ProteomeBasicRequest {

    @Parameter(description = QUERY_PROTEOME_DESCRIPTION, example = QUERY_PROTEOME_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.PROTEOME,
            messagePrefix = "search.proteome")
    private String query;

    @Parameter(description = SORT_PROTEOME_DESCRIPTION, example = SORT_PROTEOME_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.PROTEOME)
    private String sort;

    @Parameter(description = FIELDS_PROTEOME_DESCRIPTION, example = FIELDS_PROTEOME_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.PROTEOME)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
