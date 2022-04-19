package org.uniprot.api.common.repository.search;

import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.uniprot.api.common.repository.search.SolrQueryConfigFileReader.QUERY_PLACEHOLDER;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Builder
@Getter
@ToString
public class SolrQueryConfig {
    @Singular private final List<String> defaultSearchBoosts;
    private final String defaultSearchBoostFunctions;
    @Singular private final List<String> advancedSearchBoosts;
    private final String advancedSearchBoostFunctions;

    @Setter(value = AccessLevel.NONE)
    private List<String> fieldBoosts;
    @Setter(value = AccessLevel.NONE)
    private List<String> staticBoosts;

    // TODO: 14/04/2022 probably remove defaultSearchBoosts and add directly to fieldBoosts and
    // staticBoosts
    public void initialiseStaticAndFieldBoosts() {
        fieldBoosts = new ArrayList<>();
        staticBoosts = new ArrayList<>();

        for (String boost : defaultSearchBoosts) {
            if (boost.contains(QUERY_PLACEHOLDER)) {
                fieldBoosts.add(boost);
            } else {
                staticBoosts.add(boost);
            }
        }
    }

    @Setter(AccessLevel.NONE)
    private String queryFields;

    @Setter(AccessLevel.NONE)
    private Set<String> stopWords;

    @Setter(AccessLevel.NONE)
    private String highlightFields;

    public static class SolrQueryConfigBuilder {
        public SolrQueryConfigBuilder queryFields(String queryFields) {
            this.queryFields =
                    Arrays.stream(queryFields.split(","))
                            .map(String::trim)
                            .collect(Collectors.joining(" "));
            return this;
        }

        public SolrQueryConfigBuilder stopWords(String stopWords) {
            this.stopWords =
                    Arrays.stream(stopWords.split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
            return this;
        }
    }
}
