package org.uniprot.api.common.repository.search;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Slf4j
public class QueryBoostsFileReader {
    private static final String COMMENT_PREFIX = "#";
    private final QueryBoosts.QueryBoostsBuilder builder;
    private String boostsResourceLocation;

    public QueryBoostsFileReader(String boostsResourceLocation) {
        this.boostsResourceLocation = boostsResourceLocation;
        this.builder = QueryBoosts.builder();
        initialiseBoosts();
    }

    public QueryBoosts getQueryBoosts() {
        return builder.build();
    }

    private void initialiseBoosts() {
        try {
            log.info("Loading boosts ...");
            Path path = Paths.get(requireNonNull(getClass()
                                                         .getClassLoader()
                                                         .getResource(boostsResourceLocation)).toURI());

            BoostType boostType = BoostType.DEFAULT_SEARCH;
            try (Stream<String> lines = Files.lines(path)) {
                for (String line : lines.toArray(String[]::new)) {
                    if (line.startsWith(BoostType.DEFAULT_SEARCH.prefix)) {
                        boostType = BoostType.DEFAULT_SEARCH;
                    } else if (line.startsWith(BoostType.DEFAULT_SEARCH_FUNCTIONS.prefix)) {
                        boostType = BoostType.DEFAULT_SEARCH_FUNCTIONS;
                    } else if (line.startsWith(BoostType.ADVANCED_SEARCH.prefix)) {
                        boostType = BoostType.ADVANCED_SEARCH;
                    } else if (line.startsWith(BoostType.ADVANCED_SEARCH_FUNCTIONS.prefix)) {
                        boostType = BoostType.ADVANCED_SEARCH_FUNCTIONS;
                    } else if (line.startsWith(COMMENT_PREFIX) || line.isEmpty()) {
                        // => commented out or empty line, skip it
                        log.debug("ignoring boost line: <{}>", line);
                    } else {
                        addBoost(boostType, line);
                    }
                }
            }
            log.info("Loaded boosts.");
        } catch (URISyntaxException | IOException | NullPointerException e) {
            log.error("Error loading boosts.");
            throw new QueryBoostCreationException("Could not create QueryBoost", e);
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
        }
    }

    private enum BoostType {
        DEFAULT_SEARCH("# DEFAULT-SEARCH-BOOSTS"),
        DEFAULT_SEARCH_FUNCTIONS("# DEFAULT-SEARCH-BOOST-FUNCTIONS"),
        ADVANCED_SEARCH("# ADVANCED-SEARCH-BOOSTS"),
        ADVANCED_SEARCH_FUNCTIONS("# ADVANCED-SEARCH-BOOST-FUNCTIONS");

        private String prefix;

        BoostType(String prefix) {
            this.prefix = prefix;
        }
    }
}
