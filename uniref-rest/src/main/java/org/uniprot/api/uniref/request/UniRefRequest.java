package org.uniprot.api.uniref.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Data
public class UniRefRequest implements SearchRequest {

    public static final String DEFAULT_FIELDS = "id,name,common_taxon,count,created";

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(uniProtDataType = UniProtDataType.UNIREF, messagePrefix = "search.uniref")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIREF)
    private String sort;

    private String cursor;

    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIREF)
    private String fields;

    @ValidFacets(facetConfig = UniRefFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;
}
