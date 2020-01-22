package org.uniprot.api.literature.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Data
public class LiteratureRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            fieldValidatorClazz = UniProtSearchFields.class,
            enumValueName = "LITERATURE",
            messagePrefix = "search.literature")
    private String query;

    @ValidSolrSortFields(
            sortFieldEnumClazz = UniProtSearchFields.class,
            enumValueName = "LITERATURE")
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private Integer size;

    @ValidFacets(facetConfig = LiteratureFacetConfig.class)
    private String facets;
}
