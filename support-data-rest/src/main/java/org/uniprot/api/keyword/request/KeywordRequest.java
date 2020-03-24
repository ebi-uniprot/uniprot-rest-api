package org.uniprot.api.keyword.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class KeywordRequest implements SearchRequest {

    @Parameter(description = "Criteria to search the keywords. It can take any valid solr query.")
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.KEYWORD,
            messagePrefix = "search.keyword")
    private String query;

    @Parameter(description = "Name of the field to be sorted on")
    @ValidSolrSortFields(uniProtDataType = UniProtDataType.KEYWORD)
    private String sort;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Comma separated list of fields to be returned in response")
<<<<<<< HEAD:support-data-rest/src/main/java/org/uniprot/api/keyword/request/KeywordRequest.java
    @ModelFieldMeta(path = "support-data-rest/src/main/resources/keyword_return_field_meta.json")
    @ValidReturnFields(uniProtDataType = UniProtDataType.KEYWORD)
=======
    @ValidReturnFields(fieldValidatorClazz = KeywordField.ResultFields.class)
>>>>>>> initial commit towards use of flat config files to geenerate open api swagger:support-data-rest/src/main/java/org/uniprot/api/keyword/request/KeywordRequestDTO.java
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    private Integer size;

    @Override
    public String getFacets() {
        return "";
    }
}
