package uk.ac.ebi.uniprot.api.keyword.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.search.field.KeywordField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class KeywordRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = KeywordSolrQueryFieldValidator.class)
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = KeywordField.Sort.class)
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = KeywordReturnFieldsValidator.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = DEFAULT_RESULTS_SIZE;

    @Override
    public String getFacets() {
        return "";
    }
}
