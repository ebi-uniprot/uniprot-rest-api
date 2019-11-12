package org.uniprot.api.literature.request;

import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
public class LiteratureMappedRequestDTO implements SearchRequest {

    @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private Integer size;

    private String cursor;

    @ValidSolrSortFields(sortFieldEnumClazz = LiteratureField.Sort.class)
    private String sort;

    private String query; // it will be hidden

    @Override
    public String getFacets() {
        return "";
    }
}
