package org.uniprot.api.common.repository.search;

import static org.uniprot.core.util.Utils.notNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Slf4j
public class QueryBoostsFileReader {
    private static final String COMMENT_PREFIX = "#";
    private final QueryBoosts.QueryBoostsBuilder builder;
    private final String boostsResourceLocation;

    public QueryBoostsFileReader(String boostsResourceLocation) {
        this.boostsResourceLocation = boostsResourceLocation;
        this.builder = QueryBoosts.builder();
        initialiseBoosts();
    }

    public QueryBoosts getQueryBoosts() {
        return builder.build();
    }

    private void initialiseBoosts() {
        log.info("Loading boosts [" + this.boostsResourceLocation + "]...");
        InputStream resourceAsStream = getClass().getResourceAsStream(boostsResourceLocation);
        if (notNull(resourceAsStream)) {

            Stream<String> lines =
                    new BufferedReader(new InputStreamReader(resourceAsStream)).lines();
            BoostType boostType = BoostType.DEFAULT_SEARCH;
            for (String line : lines.toArray(String[]::new)) {
                if (line.startsWith(BoostType.DEFAULT_SEARCH.prefix)) {
                    boostType = BoostType.DEFAULT_SEARCH;
                } else if (line.startsWith(BoostType.DEFAULT_SEARCH_FUNCTIONS.prefix)) {
                    boostType = BoostType.DEFAULT_SEARCH_FUNCTIONS;
                } else if (line.startsWith(BoostType.ADVANCED_SEARCH.prefix)) {
                    boostType = BoostType.ADVANCED_SEARCH;
                } else if (line.startsWith(BoostType.ADVANCED_SEARCH_FUNCTIONS.prefix)) {
                    boostType = BoostType.ADVANCED_SEARCH_FUNCTIONS;
                } else if (line.startsWith(BoostType.QUERY_FIELDS.prefix)) {
                    boostType = BoostType.QUERY_FIELDS;
                } else if (line.startsWith(COMMENT_PREFIX) || line.trim().isEmpty()) {
                    // => commented out or empty line, skip it
                    log.debug("ignoring boost line: <{}>", line);
                } else {
                    addBoost(boostType, line);
                }
            }
            log.info("Loaded boosts.");
        } else {
            log.error("Error loading boosts.");
            throw new QueryBoostCreationException("Could not create QueryBoost");
        }
    }

    private void addBoost(BoostType boostType, String line) {
        switch (boostType) {
            case DEFAULT_SEARCH:
                builder.defaultSearchBoost(line);
                break;
            case DEFAULT_SEARCH_FUNCTIONS:
                builder.defaultSearchBoostFunctions(line);
                break;
            case ADVANCED_SEARCH:
                builder.advancedSearchBoost(line);
                break;
            case ADVANCED_SEARCH_FUNCTIONS:
                builder.advancedSearchBoostFunctions(line);
                break;
            case QUERY_FIELDS:
                builder.queryFields(line);
                break;
        }
    }

    private enum BoostType {
        DEFAULT_SEARCH("# DEFAULT-SEARCH-BOOSTS"),
        DEFAULT_SEARCH_FUNCTIONS("# DEFAULT-SEARCH-BOOST-FUNCTIONS"),
        QUERY_FIELDS("# QUERY-FIELDS"),
        ADVANCED_SEARCH("# ADVANCED-SEARCH-BOOSTS"),
        ADVANCED_SEARCH_FUNCTIONS("# ADVANCED-SEARCH-BOOST-FUNCTIONS");

        private final String prefix;

        BoostType(String prefix) {
            this.prefix = prefix;
        }
    }
}
