package org.uniprot.api.support.data.taxonomy.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.QueryFieldMetaReaderImpl;
import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.SortFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

@Data
public class TaxonomyRequest implements SearchRequest {

    @ModelFieldMeta(reader = QueryFieldMetaReaderImpl.class, path = "taxonomy-search-fields.json")
    @Parameter(description = "Criteria to search taxonomies. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.TAXONOMY,
            messagePrefix = "search.taxonomy")
    private String query;

    @ModelFieldMeta(reader = SortFieldMetaReaderImpl.class, path = "taxonomy-search-fields.json")
    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String sort;

    @Parameter(hidden = true)
    private String cursor;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "taxonomy-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
    private String fields;

    @Parameter(description = "Comma separated list of facets to search")
    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    private String facets;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;
}
