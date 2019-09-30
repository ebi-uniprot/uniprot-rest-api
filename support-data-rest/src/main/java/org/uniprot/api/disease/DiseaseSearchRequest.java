package org.uniprot.api.disease;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.DiseaseField;

@Data
public class DiseaseSearchRequest implements SearchRequest {
    private static final int DEFAULT_RESULTS_SIZE = 25;

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            fieldValidatorClazz = DiseaseField.Search.class,
            messagePrefix = "search.disease")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = DiseaseField.Sort.class)
    private String sort;

    private String cursor;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @ValidReturnFields(fieldValidatorClazz = DiseaseField.ResultFields.class)
    private String fields;

    @Override
    public String getFacets() {
        return "";
    }
}
