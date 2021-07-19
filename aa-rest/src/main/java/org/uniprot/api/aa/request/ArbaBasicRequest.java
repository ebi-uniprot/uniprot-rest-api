package org.uniprot.api.aa.request;

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
 * @created 19/07/2021
 */
@Data
public class ArbaBasicRequest {
    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "arba-search-fields.json")
    @Parameter(description = "Criteria to search ARBA rules. It can take any valid lucene query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIRULE, // FIXME change data type to ARBA
            messagePrefix = "search.arba")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "arba-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(
            uniProtDataType = UniProtDataType.UNIRULE) // FIXME change data type to ARBA
    private String sort;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "arba-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIRULE) // FIXME change data type to ARBA
    private String fields;
}
