package org.uniprot.api.rest.request;

import java.util.List;

import org.uniprot.store.search.SolrQueryUtil;

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
    public static boolean needsToFilterIsoform(
            String accessionIdField,
            String isIsoformField,
            String query,
            boolean isIncludeIsoform) {
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
}
