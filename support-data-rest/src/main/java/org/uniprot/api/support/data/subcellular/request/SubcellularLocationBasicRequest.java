package org.uniprot.api.support.data.subcellular.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.NotNull;

import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author sahmad
 * @created 22/01/2021
 */
@Data
public class SubcellularLocationBasicRequest implements BasicRequest {

    @Parameter(description = QUERY_SUBCEL_DESCRIPTION, example = QUERY_SUBCEL_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.SUBCELLLOCATION,
            messagePrefix = "search.subcellularLocation")
    private String query;

    @Parameter(description = SORT_SUBCEL_DESCRIPTION, example = SORT_SUBCEL_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.SUBCELLLOCATION)
    private String sort;

    @Parameter(description = FIELDS_SUBCEL_DESCRIPTION, example = FIELDS_SUBCEL_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.SUBCELLLOCATION)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
