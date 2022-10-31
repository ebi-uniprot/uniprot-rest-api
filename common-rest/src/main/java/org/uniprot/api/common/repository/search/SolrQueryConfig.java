package org.uniprot.api.common.repository.search;

import static org.uniprot.api.common.repository.search.SolrQueryConfigFileReader.QUERY_PLACEHOLDER;

import java.util.*;
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
    private final String boostFunctions;

    @Setter(value = AccessLevel.NONE)
    private List<String> fieldBoosts;

    @Setter(value = AccessLevel.NONE)
    private List<String> staticBoosts;

    @Setter(AccessLevel.NONE)
    private String queryFields;

    @Setter(AccessLevel.NONE)
    private Set<String> stopWords;

    @Setter(AccessLevel.NONE)
    private String highlightFields;

    @Setter(AccessLevel.NONE)
    private Set<String> leadingWildcardFields; // fields which support leading wildcard

    public static class SolrQueryConfigBuilder {
        // do not make final because Lombok doesn't like it
        private List<String> fieldBoosts = new ArrayList<>();
        private List<String> staticBoosts = new ArrayList<>();

        public SolrQueryConfigBuilder addBoost(String boost) {
            if (boost.contains(QUERY_PLACEHOLDER)) {
                fieldBoosts.add(boost);
            } else {
                staticBoosts.add(boost);
            }
            return this;
        }

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

        public SolrQueryConfigBuilder leadingWildcardFields(String leadingWildcardFields) {
            this.leadingWildcardFields =
                    Arrays.stream(leadingWildcardFields.split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet());
            return this;
        }
    }
}
