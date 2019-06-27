package uk.ac.ebi.uniprot.api.taxonomy.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.*;
import uk.ac.ebi.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import uk.ac.ebi.uniprot.search.field.TaxonomyField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

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
