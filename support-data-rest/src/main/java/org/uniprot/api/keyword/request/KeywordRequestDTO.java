package org.uniprot.api.keyword.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.field.KeywordField;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

@Data
public class KeywordRequestDTO implements SearchRequest {

    @Parameter(description = "Criteria to search the keywords. It can take any valid solr query.")
    @ModelFieldMeta(path = "support-data-rest/src/main/resources/keyword_query_param_meta.json")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.KEYWORD,
            messagePrefix = "search.keyword")
    private String query;

    @Parameter(description = "Name of the field to be sorted on")
    @ModelFieldMeta(path = "support-data-rest/src/main/resources/keyword_sort_param_meta.json")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.KEYWORD)
    private String sort;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ModelFieldMeta(path = "support-data-rest/src/main/resources/keyword_return_field_meta.json")
    @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Override
    public String getFacets() {
        return "";
    }
}
