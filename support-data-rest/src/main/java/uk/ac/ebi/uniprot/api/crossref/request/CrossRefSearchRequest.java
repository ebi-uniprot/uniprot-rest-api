package uk.ac.ebi.uniprot.api.crossref.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.crossref.config.CrossRefFacetConfig;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.*;
import uk.ac.ebi.uniprot.search.field.CrossRefField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class CrossRefSearchRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = CrossRefField.Search.class, messagePrefix = "search.crossref")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = CrossRefField.Sort.class)
    private String sort;

    private String cursor;

    @ValidFacets(facetConfig = CrossRefFacetConfig.class)
    private String facets;

    @ValidReturnFields(fieldValidatorClazz = CrossRefField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

}
