package org.uniprot.api.subcell.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

@Data
public class SubcellularLocationRequest implements SearchRequest {

    @ModelFieldMeta(
            reader = QueryFieldMetaReaderImpl.class,
            path = "subcelllocation-search-fields.json")
    @Parameter(
            description =
                    "Criteria to search Subcellular locations. It can take any valid solr query.")
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

    @Parameter(hidden = true)
    private String cursor;

    @ModelFieldMeta(
            reader = ReturnFieldMetaReaderImpl.class,
            path = "subcelllocation-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.SUBCELLLOCATION)
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Parameter(hidden = true)
    @Override
    public String getFacets() {
        return "";
    }
}
