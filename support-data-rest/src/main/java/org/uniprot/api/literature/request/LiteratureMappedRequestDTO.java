package org.uniprot.api.literature.request;

import lombok.Data;

import javax.validation.constraints.Positive;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidSolrSortFields;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
public class LiteratureMappedRequestDTO {

    @ValidReturnFields(fieldValidatorClazz = LiteratureField.ResultFields.class)
    private String fields;

    @Positive(message = "{search.positive}")
    private int size = SearchRequest.DEFAULT_RESULTS_SIZE;

    private String cursor;

    @ValidSolrSortFields(sortFieldEnumClazz = LiteratureField.Sort.class)
    private String sort;

}
