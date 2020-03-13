package org.uniprot.api.taxonomy.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.store.config.UniProtDataType;

@Data
public class TaxonomyRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.TAXONOMY,
            messagePrefix = "search.taxonomy")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String sort;

    private String cursor;

    @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String fields;

    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;
}
