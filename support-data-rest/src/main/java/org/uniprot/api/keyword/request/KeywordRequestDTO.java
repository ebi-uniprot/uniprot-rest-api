package org.uniprot.api.keyword.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import ebi.ac.uk.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.KeywordField;

import lombok.Data;

@Data
public class KeywordRequestDTO implements SearchRequest {

    @Parameter(description = "Criteria to search the keywords. It can take any valid solr query.")
    @ModelFieldMeta(path = "src/main/resources/keyword_query_param_meta.json")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = KeywordField.Search.class, messagePrefix = "search.keyword")
    private String query;

    @Parameter(description = "Name of the field to be sorted on")
    @ModelFieldMeta(path = "src/main/resources/keyword_sort_param_meta.json")
    @ValidSolrSortFields(sortFieldEnumClazz = KeywordField.Sort.class)
    private String sort;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ModelFieldMeta(path = "src/main/resources/keyword_return_field_meta.json")
    @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @Override
    public String getFacets() {
        return "";
    }
}
