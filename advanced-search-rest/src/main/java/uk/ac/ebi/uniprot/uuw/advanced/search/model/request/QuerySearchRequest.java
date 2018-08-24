package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;



import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;

/**
 * Search request Entity
 *
 * @author lgonzales
 */
@Data
public class QuerySearchRequest extends QueryRequest {

    @PositiveOrZero(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive.or.zero}")
    private Long offset;

    @Positive(message = "{uk.ac.ebi.uniprot.uuw.advanced.search.positive}")
    private Integer size;


}
