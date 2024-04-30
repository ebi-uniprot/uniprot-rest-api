package org.uniprot.api.support.data.crossref.request;

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
 * @created 01/02/2021
 */
@Data
public class CrossRefBasicRequest {

    @Parameter(description = QUERY_CROSSREF_DESCRIPTION, example = QUERY_CROSSREF_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.CROSSREF,
            messagePrefix = "search.crossref")
    private String query;

    @Parameter(description = SORT_CROSSREF_DESCRIPTION, example = SORT_CROSSREF_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.CROSSREF)
    private String sort;

    @Parameter(description = FIELDS_CROSSREF_DESCRIPTION, example = FIELDS_CROSSREF_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.CROSSREF)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
