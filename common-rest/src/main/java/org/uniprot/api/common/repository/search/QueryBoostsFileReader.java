package org.uniprot.api.common.repository.search;

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
public class QueryBoostsFileReader {
    private final QueryBoosts.QueryBoostsBuilder builder;
    private String boostsResourceLocation;

    public QueryBoostsFileReader(String boostsResourceLocation) {
        this.boostsResourceLocation = boostsResourceLocation;
        this.builder = QueryBoosts.builder();
        // TODO: 04/09/19 test this
    }

    public QueryBoosts getQueryBoosts() {
        return builder.build();
    }

    private void initialiseBoosts() {
        try {
            Path path = Paths.get(requireNonNull(getClass()
                                                         .getClassLoader()
                                                         .getResource(boostsResourceLocation)).toURI());

            BoostType boostType = BoostType.DEFAULT;
            try (Stream<String> lines = Files.lines(path)) {
                for (String line : lines.toArray(String[]::new)) {
                    if (line.startsWith("# DEFAULT")) {
                        boostType = BoostType.DEFAULT;
                    } else if (line.startsWith("# FIELD")) {
                        boostType = BoostType.FIELD;
                    } else if (line.startsWith("# VALUE")) {
                        boostType = BoostType.VALUE;
                    } else {
                        addBoost(boostType, line);
                    }
                }
            }
        } catch (URISyntaxException | IOException e) {
            throw new QueryBoostCreationException("Could not create QueryBoost", e);
        }
    }

    private void addBoost(BoostType boostType, String line) {
        switch (boostType) {
            case DEFAULT:
                builder.defaultBoost(line);
                break;
            case FIELD:
                builder.fieldBoost(line);
                break;
            case VALUE:
                builder.valueBoost(line);
                break;
            case FUNCTION:
                builder.boostFunction(line);
                break;
        }
    }

    private BoostType getBoostType(String line, BoostType defaultType) {
        if (line.startsWith("# DEFAULT")) {
            return BoostType.DEFAULT;
        } else if (line.startsWith("# FIELD")) {
            return BoostType.FIELD;
        } else if (line.startsWith("# VALUE")) {
            return BoostType.VALUE;
        } else {
            return defaultType;
        }
    }

    private enum BoostType {
        DEFAULT, FIELD, VALUE, FUNCTION
    }
}
