package uk.ac.ebi.uniprot.api.literature.request;

import lombok.Data;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.ValidReturnFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.search.field.LiteratureField;

import javax.validation.constraints.Positive;

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
