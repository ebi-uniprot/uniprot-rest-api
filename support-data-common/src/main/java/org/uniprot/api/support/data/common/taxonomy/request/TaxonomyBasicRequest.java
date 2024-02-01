package org.uniprot.api.support.data.common.taxonomy.request;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.openapi.OpenApiConstants;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

/**
 * @author sahmad
 * @created 23/01/2021
 */
@Data
public class TaxonomyBasicRequest {

    @Parameter(description = QUERY_TAX_DESCRIPTION, example = QUERY_TAX_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.TAXONOMY,
            messagePrefix = "search.taxonomy")
    private String query;

    @Parameter(description = SORT_TAX_DESCRIPTION, example = SORT_TAX_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String sort;

    @Parameter(description = FIELDS_TAX_DESCRIPTION, example = FIELDS_TAX_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
