package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.rest.openapi.OpenAPIConstants.FIELDS_UNIPARC_EXAMPLE;

import javax.validation.constraints.NotNull;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 12/08/2020
 */
@Data
@ParameterObject
public class UniParcBestGuessRequest {

    @Parameter(description = QUERY_UNIPARC_DESCRIPTION, example = QUERY_UNIPARC_EXAMPLE)
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String fields;
}
