package org.uniprot.api.common.repository.search.facet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author lgonzales
 * @since 26/08/2020
 */
public class FacetUtils {

    private static final Pattern CLEAN_VALUE_REGEX = Pattern.compile("[a-zA-Z_]");

    private FacetUtils() {}

    public static List<FacetItem> buildFacetItems(Map<String, Long> countMap) {
        List<FacetItem> result = new ArrayList<>();

        countMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEachOrdered(
                        entry-> {
                            String value = getCleanFacetValue(entry.getKey());
                            FacetItem item =
                                    FacetItem.builder()
                                            .label(entry.getKey())
                                            .value(value)
                                            .count(entry.getValue())
                                            .build();
                            result.add(item);
                        });

        return result;
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
}
