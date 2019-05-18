package uk.ac.ebi.uniprot.api.suggester;

import lombok.Builder;
import lombok.Getter;

/**
 * Created 17/12/18
 *
 * @author Edd
 */
@Getter
@Builder
public class Suggestion {
    private String value;
    private String id;
}
