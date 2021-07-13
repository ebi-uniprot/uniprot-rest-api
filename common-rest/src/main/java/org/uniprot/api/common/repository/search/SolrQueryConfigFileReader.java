package org.uniprot.api.common.repository.search;

import static org.uniprot.core.util.Utils.notNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Slf4j
public class SolrQueryConfigFileReader {
    private static final String COMMENT_PREFIX = "#";
    private final SolrQueryConfig.SolrQueryConfigBuilder builder;
    private final String resourceLocation;

    public SolrQueryConfigFileReader(String resourceLocation) {
        this.resourceLocation = resourceLocation;
        this.builder = SolrQueryConfig.builder();
        initialiseSolrQueryConfig();
    }

    public SolrQueryConfig getConfig() {
        return builder.build();
    }

    private void initialiseSolrQueryConfig() {
        log.info("Loading Solr query config [" + this.resourceLocation + "]...");
        InputStream resourceAsStream = getClass().getResourceAsStream(resourceLocation);
        if (notNull(resourceAsStream)) {

            Stream<String> lines =
                    new BufferedReader(new InputStreamReader(resourceAsStream)).lines();
            QueryConfigType queryConfigType = QueryConfigType.DEFAULT_SEARCH;
            for (String line : lines.toArray(String[]::new)) {
                String trimmedLine = line.trim();
                QueryConfigType parsedType = QueryConfigType.typeOf(trimmedLine);
                if (parsedType == null) {
                    if ((line.startsWith(COMMENT_PREFIX) || trimmedLine.isEmpty())) {
                        // => commented out or empty line, skip it
                        log.debug("ignoring boost line: <{}>", line);
                    } else {
                        addQueryConfig(queryConfigType, trimmedLine);
                    }
                } else {
                    queryConfigType = parsedType;
                }
            }
            log.info("Loaded Solr query config.");
        } else {
            log.error("Error loading Solr query config.");
            throw new SolrQueryConfigCreationException("Could not create Solr query configuration");
        }
    }

    private void addQueryConfig(QueryConfigType queryConfigType, String line) {
        switch (queryConfigType) {
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
            case STOP_WORDS:
                builder.stopWords(line);
                break;
            case HIGHLIGHT_FIELDS:
                builder.highlightFields(line);
                break;
        }
    }

    private enum QueryConfigType {
        DEFAULT_SEARCH("# DEFAULT-SEARCH-BOOSTS"),
        DEFAULT_SEARCH_FUNCTIONS("# DEFAULT-SEARCH-BOOST-FUNCTIONS"),
        QUERY_FIELDS("# QUERY-FIELDS"),
        ADVANCED_SEARCH("# ADVANCED-SEARCH-BOOSTS"),
        ADVANCED_SEARCH_FUNCTIONS("# ADVANCED-SEARCH-BOOST-FUNCTIONS"),
        STOP_WORDS("# STOP-WORDS"),
        HIGHLIGHT_FIELDS("# HIGHLIGHT-FIELDS");

        private final String prefix;

        QueryConfigType(String prefix) {
            this.prefix = prefix;
        }

        private static QueryConfigType typeOf(String value) {
            return Arrays.stream(QueryConfigType.values())
                    .filter(val -> val.prefix.equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }
}
