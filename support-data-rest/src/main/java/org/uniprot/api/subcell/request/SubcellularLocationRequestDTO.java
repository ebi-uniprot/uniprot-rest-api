package org.uniprot.api.subcell.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.SubcellularLocationField;

@Data
public class SubcellularLocationRequestDTO implements SearchRequest {

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            fieldValidatorClazz = UniProtSearchFields.class,
            enumValueName = "SUBCELL",
            messagePrefix = "search.subcellularLocation")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtSearchFields.class, enumValueName = "SUBCELL")
    private String sort;

    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = SubcellularLocationField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private Integer size;

    @Override
    public String getFacets() {
        return "";
    }
}
