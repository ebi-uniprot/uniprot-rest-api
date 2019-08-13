package org.uniprot.api.suggester;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
@Builder
@Getter
public class Suggestions {
    private final String query;
    private final String dictionary;
    private final List<Suggestion> suggestions;
}
