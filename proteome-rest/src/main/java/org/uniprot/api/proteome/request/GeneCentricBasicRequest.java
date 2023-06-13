package org.uniprot.api.proteome.request;

import javax.validation.constraints.NotNull;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 29/10/2020
 */
@Data
public class GeneCentricBasicRequest {

    @ModelFieldMeta(
            reader = QueryFieldMetaReaderImpl.class,
            path = "genecentric-search-fields.json")
    @Parameter(
            description =
                    "Criteria to search the gene centric data set. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.GENECENTRIC,
            messagePrefix = "search.genecentric")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "genecentric-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String sort;

    @ModelFieldMeta(
            reader = ReturnFieldMetaReaderImpl.class,
            path = "genecentric-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String fields;

    private String format;
}
