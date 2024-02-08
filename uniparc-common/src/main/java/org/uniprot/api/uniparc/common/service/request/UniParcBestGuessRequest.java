package org.uniprot.api.uniparc.common.service.request;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 12/08/2020
 */
@Data
public class UniParcBestGuessRequest {

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "uniparc-search-fields.json")
    @Parameter(
            description =
                    "The query is used to search UniParc. This accepts any valid standard Lucene query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    protected String query;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in the response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String fields;
}
