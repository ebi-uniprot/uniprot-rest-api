package org.uniprot.api.disease;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.DiseaseField;

@Data
public class DiseaseSearchRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.DISEASE,
            messagePrefix = "search.disease")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.DISEASE)
    private String sort;

    private String cursor;

    @Positive(message = "{search.positive}")
    private Integer size;

    @ValidReturnFields(fieldValidatorClazz = DiseaseField.ResultFields.class)
    private String fields;

    @Override
    public String getFacets() {
        return "";
    }
}
