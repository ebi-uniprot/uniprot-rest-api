package org.uniprot.api.support.data.suggester.response;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Created 17/12/18
 *
 * @author Edd
 */
@Getter
@Builder
@EqualsAndHashCode
public class Suggestion {
    private String value;
    private String id;
}
