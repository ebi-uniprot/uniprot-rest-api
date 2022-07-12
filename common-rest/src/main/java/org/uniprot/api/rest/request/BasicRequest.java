package org.uniprot.api.rest.request;

import java.util.List;

import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrQueryUtil;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
public interface BasicRequest {

    String getQuery();

    String getFields();

    String getSort();

    default boolean hasFields() {
        return Utils.notNullNotEmpty(getFields());
    }

    default boolean hasSort() {
        return Utils.notNullNotEmpty(getSort());
    }

    default boolean isIncludeIsoform() {
        return false;
    }

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
    default boolean needsToFilterIsoform(String accessionIdField, String isIsoformField) {
        return needsToFilterIsoform(accessionIdField, isIsoformField, getQuery());
    }

    default boolean needsToFilterIsoform(
            String accessionIdField, String isIsoformField, String query) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(query, accessionIdField, isIsoformField);

        List<String> accessionValues =
                SolrQueryUtil.getTermValuesWithWhitespaceAnalyzer(query, "accession");
        boolean hasIsoforms =
                !accessionValues.isEmpty()
                        && accessionValues.stream().allMatch(acc -> acc.contains("-"));

        if (!hasIdFieldTerms && !hasIsoforms) {
            return !isIncludeIsoform();
        } else {
            return false;
        }
    }
}
