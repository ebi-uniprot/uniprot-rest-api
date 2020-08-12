package org.uniprot.api.common.repository.search;

import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Builder
@Getter
@ToString
public class QueryBoosts {
    @Singular private List<String> defaultSearchBoosts;
    private String defaultSearchBoostFunctions;
    @Singular private List<String> advancedSearchBoosts;
    private String advancedSearchBoostFunctions;

    @Setter(AccessLevel.NONE)
    private String queryFields;

    public static class QueryBoostsBuilder {
        public QueryBoostsBuilder queryFields(String queryFields) {
            this.queryFields =
                    Arrays.stream(queryFields.split(","))
                            .map(String::trim)
                            .collect(Collectors.joining(" "));
            return this;
        }
    }
}
