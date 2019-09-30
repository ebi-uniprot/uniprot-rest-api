package org.uniprot.api.keyword.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.KeywordField;

@Data
public class KeywordRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            fieldValidatorClazz = KeywordField.Search.class,
            messagePrefix = "search.keyword")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = KeywordField.Sort.class)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @Override
    public String getFacets() {
        return "";
    }
}
