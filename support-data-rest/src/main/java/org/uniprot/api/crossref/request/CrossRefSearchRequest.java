package org.uniprot.api.crossref.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.crossref.config.CrossRefFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.field.CrossRefField;

@Data
public class CrossRefSearchRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.CROSSREF,
            messagePrefix = "search.crossref")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.CROSSREF)
    private String sort;

    private String cursor;

    @ValidFacets(facetConfig = CrossRefFacetConfig.class)
    private String facets;

    @ValidReturnFields(fieldValidatorClazz = CrossRefField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private Integer size;
}
