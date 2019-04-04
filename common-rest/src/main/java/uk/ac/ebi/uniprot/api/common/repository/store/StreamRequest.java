package uk.ac.ebi.uniprot.api.common.repository.store;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Sort;
import uk.ac.ebi.uniprot.common.Utils;

/**
 * Represents the request object for download stream request
 *
 * @author lgonzales
 */
@Data
@Builder
public class StreamRequest {

    private String query;

    private String filterQuery;

    private Sort sort;

    private String defaultQueryOperator;

    boolean hasFilterQuery(){
       return Utils.notEmpty(this.filterQuery);
    }

    boolean hasDefaultQueryOperator(){
        return Utils.notEmpty(this.defaultQueryOperator);
    }

}
