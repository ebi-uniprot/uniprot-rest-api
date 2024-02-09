package org.uniprot.api.support.data.common.keyword.request;

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
 * @created 21/01/2021
 */
@Data
public class KeywordBasicRequest {
    @Parameter(description = QUERY_KEYWORDS_DESCRIPTION, example = QUERY_KEYWORDS_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.KEYWORD,
            messagePrefix = "search.keyword")
    private String query;

    @Parameter(description = SORT_KEYWORDS_DESCRIPTION, example = SORT_KEYWORDS_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.KEYWORD)
    private String sort;

    @Parameter(description = FIELDS_KEYWORDS_DESCRIPTION, example = FIELDS_KEYWORDS_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.KEYWORD)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
