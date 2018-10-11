package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Search cursor request Entity
 *
 * @author lgonzales
 */
@Data
public class SearchRequestDTO {

    @NotNull(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.required}")
    private String query;

    private String fields;

    private String sort;

    private String cursor;

    @Positive(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive}")
    private Integer size;

}
