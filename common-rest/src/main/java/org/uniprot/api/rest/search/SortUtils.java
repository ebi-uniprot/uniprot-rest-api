package org.uniprot.api.rest.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * @author lgonzales
 * @since 22/02/2021
 */
public class SortUtils {

    private SortUtils() {}

    public static List<SolrQuery.SortClause> parseSortClause(
            UniProtDataType dataType, String sortClause) {
        List<SolrQuery.SortClause> fieldSortPairs = new ArrayList<>();

        String[] tokenizedSortClause =
                sortClause.split(","); // e.g. field1 asc, field2 desc, field3 asc

        for (String singleSortPairStr : tokenizedSortClause) {
            String[] fieldSortPairArr = singleSortPairStr.strip().split("\\s+");
            if (fieldSortPairArr.length != 2) {
                throw new IllegalArgumentException("You must pass the field and sort direction.");
            }
            String solrFieldName = getSolrSortFieldName(dataType, fieldSortPairArr[0]);

            fieldSortPairs.add(
                    SolrQuery.SortClause.create(
                            solrFieldName, SolrQuery.ORDER.valueOf(fieldSortPairArr[1])));
        }
        return fieldSortPairs;
    }

    public static String getSolrSortFieldName(UniProtDataType dataType, String searchFieldName) {
        SearchFieldConfig searchFieldConfig = getSearchFieldConfig(dataType);
        return searchFieldConfig.getCorrespondingSortField(searchFieldName).getFieldName();
    }

    public static SearchFieldConfig getSearchFieldConfig(UniProtDataType dataType) {
        return SearchFieldConfigFactory.getSearchFieldConfig(dataType);
    }
}
