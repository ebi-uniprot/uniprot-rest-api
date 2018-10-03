package uk.ac.ebi.uniprot.uuw.suggester.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Created 02/10/18
 *
 * @author Edd
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Suggestion {
    String id;
    String name;

    public String toSuggestionLine() {
        return name + " [" + id + "]";
    }
}
