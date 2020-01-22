package org.uniprot.api.taxonomy.request;

import lombok.Data;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation2.ValidSolrSortFields;
import org.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.TaxonomyField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class TaxonomyRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @org.uniprot.api.rest.validation2.ValidSolrQueryFields(
            fieldValidatorClazz = UniProtSearchFields.class,
            enumValueName = "TAXONOMY",
            messagePrefix = "search.taxonomy")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtSearchFields.class, enumValueName = "TAXONOMY")
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = TaxonomyField.ResultFields.class)
    private String fields;

    @ValidFacets(facetConfig = TaxonomyFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;
}
