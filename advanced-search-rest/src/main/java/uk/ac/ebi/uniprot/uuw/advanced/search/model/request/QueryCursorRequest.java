package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import javax.validation.constraints.Positive;

import lombok.Data;

/**
 * Search cursor request Entity
 *
 * @author lgonzales
 */
@Data
public class QueryCursorRequest extends QueryRequest {

    private String cursor;

    @Positive(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive}")
    private Integer size;

}
