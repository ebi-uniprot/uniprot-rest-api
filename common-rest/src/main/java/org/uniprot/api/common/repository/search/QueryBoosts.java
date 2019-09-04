package org.uniprot.api.common.repository.search;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Builder
@Getter
@ToString
public class QueryBoosts {
    @Singular
    private List<String> defaultSearchBoosts;
    @Singular
    private List<String> defaultSearchBoostFunctions;
    @Singular
    private List<String> advancedSearchBoosts;
    @Singular
    private List<String> advancedSearchBoostFunctions;
}
