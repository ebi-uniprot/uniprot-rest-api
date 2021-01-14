package org.uniprot.api.support.data.suggester.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

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
