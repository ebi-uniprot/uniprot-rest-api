package org.uniprot.api.rest.service;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.uniprot.core.util.Utils;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * @author lgonzales
 * @since 10/06/2020
 */
public class DefaultSearchQueryOptimiser {

    private final List<SearchFieldItem> optimisableFields;
    // The optimiser first breaks the query into white space separated tokens. These tokens are then
    // matched against TOKEN_PATTERN. If there is no match, then no optimisation occurs. Otherwise,
    // the token is split into three groups: (before)(token)(after).
    // Groups 1 and 2 allows characters not related to the actual token, e.g., +,-,[,],(,), etc.
    // Group 3 records the precise value, which is not permitted to contain a ':' (i.e., no field
    // specified),
    // and is then matched against all known ID fields for an optimised version, see {@link
    // org.uniprot.api.rest.service.DefaultSearchQueryOptimiser.getOptimisedDefaultTermForValue}
    // For example: query = "nofield field1:hello (+x AND y) => tokens ["nofield", "(+x", "AND",
    // "y)"].
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("([\"+\\-\\(\\)\\]]+)?([\\w]+)([\"\\(\\)\\[]+)?");

    public DefaultSearchQueryOptimiser(SearchFieldItem idField) {
        this(Collections.singletonList(idField));
    }

    public DefaultSearchQueryOptimiser(List<SearchFieldItem> optimisableFields) {
        this.optimisableFields = optimisableFields;
    }

    /**
     * This method go through requested user query and verify if it can be optimised
     *
     * @param requestedQuery requested query
     * @return Optimised search query string
     */
    public String optimiseSearchQuery(String requestedQuery) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = requestedQuery.split("[ \t]");
        String sep = "";
        for (String rawToken : tokens) {
            Matcher matcher = TOKEN_PATTERN.matcher(rawToken);
            if (matcher.matches()
                    && !matcher.group(2).equalsIgnoreCase("AND")
                    && !matcher.group(2).equalsIgnoreCase("OR")) {

                String optimisedToken = getOptimisedDefaultTermForValue(matcher.group(2));

                String group1 = Utils.emptyOrString(matcher.group(1));
                String group3 = Utils.emptyOrString(matcher.group(3));

                // quotes
                if (group1.endsWith("\"") && group3.startsWith("\"")) {
                    group1 = group1.substring(0, group1.length() - 1);
                    group3 = group3.substring(1);
                }

                sb.append(sep).append(group1).append(optimisedToken).append(group3);
            } else {
                sb.append(sep).append(rawToken);
            }
            sep = " ";
        }

        return sb.toString();
    }

    /**
     * Method to verify if the default term query value can be optimised to use an specific search
     * field
     *
     * <p>For example: In UniProtEntryService implementation if the user type a valid Accession
     * P12345 as a default term value This method implementation would return an optimised query
     * accession:P12345
     *
     * @param termQueryValue requested default term query value
     * @return the optimised term query if can be optimised
     */
    private String getOptimisedDefaultTermForValue(String termQueryValue) {
        String toReturn = termQueryValue;
        for (SearchFieldItem field : optimisableFields) {
            if (notNullNotEmpty(field.getValidRegex())
                    && termQueryValue.matches(field.getValidRegex())) {
                toReturn = field.getFieldName() + ":" + termQueryValue.toUpperCase();
                break;
            }
        }
        return toReturn;
    }
}
