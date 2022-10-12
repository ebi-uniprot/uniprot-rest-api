package org.uniprot.api.support.data.literature.request;

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
 * @author sahmad
 * @created 22/01/2021
 */
@Data
public class LiteratureBasicRequest {
    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "literature-search-fields.json")
    @Parameter(
            description =
                    "Criteria to search literature publications. It can take any valid Lucene query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.LITERATURE,
            messagePrefix = "search.literature")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "literature-search-fields.json")
    @Parameter(hidden = true, description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.LITERATURE)
    private String sort;

    @ModelFieldMeta(
            reader = ReturnFieldMetaReaderImpl.class,
            path = "literature-return-fields.json")
    @Parameter(
            hidden = true,
            description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.LITERATURE)
    private String fields;
}
