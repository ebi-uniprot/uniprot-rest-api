package org.uniprot.api.support.data.subcellular.request;

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
public class SubcellularLocationBasicRequest {
    @ModelFieldMeta(
            reader = QueryFieldMetaReaderImpl.class,
            path = "subcelllocation-search-fields.json")
    @Parameter(
            description =
                    "Criteria to search Subcellular locations. It can take any valid Lucene query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.SUBCELLLOCATION,
            messagePrefix = "search.subcellularLocation")
    private String query;

    @ModelFieldMeta(
            reader = SortFieldMetaReaderImpl.class,
            path = "subcelllocation-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.SUBCELLLOCATION)
    private String sort;

    @ModelFieldMeta(
            reader = ReturnFieldMetaReaderImpl.class,
            path = "subcelllocation-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.SUBCELLLOCATION)
    private String fields;
}
