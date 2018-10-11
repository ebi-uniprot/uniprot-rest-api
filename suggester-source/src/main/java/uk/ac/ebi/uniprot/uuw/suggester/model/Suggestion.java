package uk.ac.ebi.uniprot.uuw.suggester.model;

import joptsimple.internal.Strings;
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
    String prefix;
    String id;
    String name;

    public String toSuggestionLine() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Cannot have empty name");
        }

        String suggestionLine = Strings.isNullOrEmpty(prefix) ? "" : prefix + ": ";
        suggestionLine = suggestionLine + (Strings.isNullOrEmpty(name) ? "NULL" : name + " ");
        suggestionLine = suggestionLine + (Strings.isNullOrEmpty(id) ? "" : "[" + id + "]");
        return suggestionLine.trim();
    }
}
