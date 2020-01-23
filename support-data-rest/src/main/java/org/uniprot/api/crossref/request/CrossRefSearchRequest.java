package org.uniprot.api.crossref.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.crossref.config.CrossRefFacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.CrossRefField;
import org.uniprot.store.search.field.UniProtSearchFields;

@Data
public class CrossRefSearchRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            fieldValidatorClazz = UniProtSearchFields.class,
            enumValueName = "CROSSREF",
            messagePrefix = "search.crossref")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtSearchFields.class, enumValueName = "CROSSREF")
    private String sort;

    private String cursor;

    @ValidFacets(facetConfig = CrossRefFacetConfig.class)
    private String facets;

    @ValidReturnFields(fieldValidatorClazz = CrossRefField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private Integer size;
}
