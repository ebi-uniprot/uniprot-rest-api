package org.uniprot.api.common.repository.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 04/09/19
 *
 * @author Edd
 */
@Slf4j
public class SolrQueryConfigFileReader {
    public static final String QUERY_PLACEHOLDER = "{query}";
    public static final String BOOST_FIELD_TYPE_NUMBER = "=number:";
    private static final String COMMENT_PREFIX = "#";
    private static final String SPRING_CONFIG_LOCATION = "spring.config.location";
    private final SolrQueryConfig.SolrQueryConfigBuilder builder;
    private final String resourceLocation;
    private DefaultResourceLoader loader;
    private static String externalConfigDir;

    static {
        // set the location of the config dir if present
        String configLoc = System.getProperty(SPRING_CONFIG_LOCATION);
        if (configLoc != null) {
            File configLocFile = Paths.get(configLoc).toFile();
            if (configLocFile.exists()) {
                if (configLocFile.isFile()) {
                    externalConfigDir = configLocFile.getParentFile().getAbsolutePath();
                } else {
                    externalConfigDir = configLocFile.getAbsolutePath();
                }
            }
        } else {
            externalConfigDir = null;
        }
    }

    public SolrQueryConfigFileReader(String resourceLocation) {
        this.resourceLocation = resourceLocation;
        this.builder = SolrQueryConfig.builder();
        initialiseSolrQueryConfig();
    }

    public SolrQueryConfig getConfig() {
        return builder.build();
    }

    private void initialiseSolrQueryConfig() {
        loader = new DefaultResourceLoader();

        String location = getConfigLocation();
        log.debug("Loading Solr query config [" + location + "]...");
        // try to query config from file first, then try from classpath
        Stream<String> lines =
                getConfigAsStringStream("file:" + location)
                        .or(() -> getConfigAsStringStream("classpath:" + location))
                        .orElseThrow(
                                () -> {
                                    log.error("Error loading Solr query config.");
                                    return new SolrQueryConfigCreationException(
                                            "Could not load config resources: " + location);
                                });

        String[] linesArr = lines.toArray(String[]::new);
        logSizeOfConfigFile(linesArr);

        QueryConfigType queryConfigType = QueryConfigType.SEARCH;
        for (String line : linesArr) {
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
        log.debug("Loaded Solr query config.");
    }

    private void logSizeOfConfigFile(String[] linesArr) {
        if (linesArr.length == 0) {
            log.warn("Solr query config is EMPTY");
        } else {
            log.debug("Solr query config contains {} lines", linesArr.length);
        }
    }

    private String getConfigLocation() {
        String location;
        if (externalConfigDir != null) {
            location = externalConfigDir + this.resourceLocation;
        } else {
            location = this.resourceLocation;
        }
        return location;
    }

    private Optional<Stream<String>> getConfigAsStringStream(String resourceLocation) {
        Resource resource = loader.getResource(resourceLocation);
        if (resource.exists()) {
            try {
                log.debug("Reading query config from [{}]", resource.getURI());
                return Optional.of(
                        new BufferedReader(new InputStreamReader(resource.getInputStream()))
                                .lines());
            } catch (IOException e) {
                log.error("Could not read query config [{}]", resourceLocation, e);
            }
        } else {
            log.warn("Could not read query config [{}]", resourceLocation);
        }
        return Optional.empty();
    }

    private void addQueryConfig(QueryConfigType queryConfigType, String line) {
        switch (queryConfigType) {
            case SEARCH:
                builder.addBoost(line);
                break;
            case SEARCH_FUNCTIONS:
                builder.boostFunctions(line);
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
            case LEADING_WILDCARD_SUPPORTED_FIELDS:
                builder.leadingWildcardFields(line);
                break;
            case EXTRA_QUERY_FIELDS:
                builder.extraOptmisableQueryFields(line);
                break;
        }
    }

    private enum QueryConfigType {
        SEARCH("# SEARCH-BOOSTS"),
        SEARCH_FUNCTIONS("# SEARCH-BOOST-FUNCTIONS"),
        QUERY_FIELDS("# QUERY-FIELDS"),
        STOP_WORDS("# STOP-WORDS"),
        HIGHLIGHT_FIELDS("# HIGHLIGHT-FIELDS"),
        LEADING_WILDCARD_SUPPORTED_FIELDS("# LEADING-WILDCARD-SUPPORTED-FIELDS"),
        EXTRA_QUERY_FIELDS("# EXTRA-QUERY-FIELDS");

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
