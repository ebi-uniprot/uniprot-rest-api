package org.uniprot.api.support.data.disease.request;

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
 * @created 20/01/2021
 */
@Data
public class DiseaseBasicRequest {

    @Parameter(description = QUERY_DISEASE_DESCRIPTION, example = QUERY_DISEASE_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.DISEASE,
            messagePrefix = "search.disease")
    private String query;

    @Parameter(description = SORT_DISEASE_DESCRIPTION, example = SORT_DISEASE_EXAMPLE)
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.DISEASE)
    private String sort;

    @Parameter(description = FIELDS_DISEASE_DESCRIPTION, example = FIELDS_DISEASE_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.DISEASE)
    private String fields;

    @Parameter(hidden = true)
    private String format;
}
