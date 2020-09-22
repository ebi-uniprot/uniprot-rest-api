package org.uniprot.api.common.repository.search;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.*;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Builder
@Getter
@ToString
public class SolrQueryConfig {
    @Singular private List<String> defaultSearchBoosts;
    private String defaultSearchBoostFunctions;
    @Singular private List<String> advancedSearchBoosts;
    private String advancedSearchBoostFunctions;

    @Setter(AccessLevel.NONE)
    private String queryFields;

    public static class SolrQueryConfigBuilder {
        public SolrQueryConfigBuilder queryFields(String queryFields) {
            this.queryFields =
                    Arrays.stream(queryFields.split(","))
                            .map(String::trim)
                            .collect(Collectors.joining(" "));
            return this;
        }
    }
}
