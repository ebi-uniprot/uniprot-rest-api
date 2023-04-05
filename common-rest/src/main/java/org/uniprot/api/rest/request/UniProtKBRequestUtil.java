package org.uniprot.api.rest.request;

import static org.uniprot.core.util.Utils.notNullNotEmpty;

import java.util.List;
import java.util.regex.Pattern;

import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

/**
 * @author sahmad
 * @created 15/07/2022
 */
public class UniProtKBRequestUtil {
    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * <p>if the query does not have accession_id field (we should not filter isoforms when querying
     * for accession_id) AND has includeIsoform params in the request URL Then we analyze the
     * includeIsoform request parameter. IMPORTANT: Implementing this way, query search has
     * precedence over isoform request parameter
     *
     * @return true if we need to add isoform filter query
     */
    private static final Pattern CLEAN_QUERY_REGEX =
            Pattern.compile(FieldRegexConstants.CLEAN_QUERY_REGEX);

    private static final Pattern ACCESSION_REGEX_ISOFORM =
            Pattern.compile(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);
    public static final String DASH = "-";

    public static boolean needsToFilterIsoform(
            String accessionIdField,
            String isIsoformField,
            String query,
            boolean isIncludeIsoform) {

        if (queryVerifiesAccessionRegexAndHasDash(query)) {
            return false;
        }
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(query, accessionIdField, isIsoformField);

        List<String> accessionValues =
                SolrQueryUtil.getTermValuesWithWhitespaceAnalyzer(query, "accession");
        boolean hasIsoforms =
                !accessionValues.isEmpty()
                        && accessionValues.stream().allMatch(acc -> acc.contains("-"));

        if (!hasIdFieldTerms && !hasIsoforms) {
            return !isIncludeIsoform;
        } else {
            return false;
        }
    }

    private static boolean queryVerifiesAccessionRegexAndHasDash(String query) {
        boolean response = false;
        if (notNullNotEmpty(query)) {
            query = CLEAN_QUERY_REGEX.matcher(query.strip()).replaceAll("");
            // We don't add is_isoform:false filter if query verifies accession regex and has dash
            if (ACCESSION_REGEX_ISOFORM.matcher(query.toUpperCase()).matches()
                    && query.contains(DASH)) {
                response = true;
            }
        }
        return response;
    }
}
