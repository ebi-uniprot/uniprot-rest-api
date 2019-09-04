package org.uniprot.api.common.repository.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Builder
@Getter
public class QueryBoosts {
    @Singular
    private List<String> defaultBoosts;
    @Singular
    private List<String> fieldBoosts;
    @Singular
    private List<String> valueBoosts;
    @Singular
    private List<String> boostFunctions;
}
