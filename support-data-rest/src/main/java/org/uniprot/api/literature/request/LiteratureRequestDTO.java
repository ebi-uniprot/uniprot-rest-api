package org.uniprot.api.literature.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.uniprot.api.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Data
public class LiteratureRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")

    @ValidSolrQueryFields(fieldValidatorClazz = LiteratureField.Search.class, messagePrefix = "search.literature")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = LiteratureField.Sort.class)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @ValidFacets(facetConfig = LiteratureFacetConfig.class)
    private String facets;
}
