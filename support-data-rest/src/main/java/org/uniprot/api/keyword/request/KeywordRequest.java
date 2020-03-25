package org.uniprot.api.keyword.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;

@Data
public class KeywordRequest implements SearchRequest {

    @Parameter(description = "Criteria to search the keywords. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.KEYWORD,
            messagePrefix = "search.keyword")
    private String query;

    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.KEYWORD)
    private String sort;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.KEYWORD)
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Override
    public String getFacets() {
        return "";
    }
}
