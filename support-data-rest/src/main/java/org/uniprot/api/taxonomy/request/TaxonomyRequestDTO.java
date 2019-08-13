package org.uniprot.api.taxonomy.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.store.search.field.TaxonomyField;

@Data
public class TaxonomyRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = TaxonomyField.Search.class, messagePrefix = "search.taxonomy")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = TaxonomyField.Sort.class)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = TaxonomyField.ResultFields.class)
    private String fields;

    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

}
