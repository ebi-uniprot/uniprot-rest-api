package uk.ac.ebi.uniprot.uuw.suggester.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Created 02/10/18
 *
 * @author Edd
 */
@Builder
@Getter
public class Suggestion {
    String id;
    String name;

    public String toSuggestionLine() {
        return name + " [" + id + "]";
    }
}
