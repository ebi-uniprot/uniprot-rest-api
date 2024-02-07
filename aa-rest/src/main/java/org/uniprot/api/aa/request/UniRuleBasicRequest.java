package org.uniprot.api.aa.request;

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
 * @created 02/12/2020
 */
@Data
public class UniRuleBasicRequest {

    @Parameter(description = QUERY_UNIRULE_DESCRIPTION, example = QUERY_UNIRULE_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIRULE,
            messagePrefix = "search.unirule")
    private String query;

    @Parameter(description = SORT_UNIRULE_DESCRIPTION, example = SORT_UNIRULE_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIRULE)
    private String sort;

    @Parameter(description = FIELDS_UNIRULE_DESCRIPTION, example = FIELDS_UNIRULE_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIRULE)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
