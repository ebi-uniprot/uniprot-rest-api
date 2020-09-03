package org.uniprot.api.common.repository.search.facet;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class used to help build facets when they are extracted manually from objects, in other
 * words when they are not build from Solr response.
 *
 * @author lgonzales
 * @since 26/08/2020
 */
public class FacetUtils {

    private static final Pattern CLEAN_VALUE_REGEX = Pattern.compile("[a-zA-Z_]");

    private FacetUtils() {}

    public static List<FacetItem> buildFacetItems(Map<String, Long> countMap) {
        return countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(FacetUtils::getFacetItem)
                .collect(Collectors.toList());
    }

    public static String getCleanFacetValue(String value) {
        StringBuilder result = new StringBuilder();
        value = value.replace(" ", "_");
        Matcher m = CLEAN_VALUE_REGEX.matcher(value);
        while (m.find()) {
            result.append(m.group());
        }
        return result.toString().toLowerCase();
    }

    private static FacetItem getFacetItem(Map.Entry<String, Long> entry) {
        String value = getCleanFacetValue(entry.getKey());
        return FacetItem.builder()
                .label(entry.getKey())
                .value(value)
                .count(entry.getValue())
                .build();
    }
}
