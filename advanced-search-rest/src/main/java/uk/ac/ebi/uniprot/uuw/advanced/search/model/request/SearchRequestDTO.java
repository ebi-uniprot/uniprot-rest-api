package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.ValidSolrSortFields;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.validator.impl.UniprotSolrQueryFieldValidator;

/**
 * Search cursor request Entity
 *
 * @author lgonzales
 */
@Data
public class SearchRequestDTO {

    @NotNull(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.required}")
    @ValidSolrQuerySyntax(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = UniprotSolrQueryFieldValidator.class)
    private String query;

    private String fields;

    @ValidSolrSortFields(sortFieldEnumClazz = UniProtField.Sort.class)
    private String sort;

    private String cursor;

    @Positive(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive}")
    private Integer size;

}
