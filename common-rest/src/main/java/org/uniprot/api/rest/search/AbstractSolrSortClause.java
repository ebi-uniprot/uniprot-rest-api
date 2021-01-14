package org.uniprot.api.rest.search;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * A class to handle solr sort clause, like parsing, providing default sort order
 *
 * @author sahmad
 */
public abstract class AbstractSolrSortClause {
    public static final String SCORE = "score";

    private final List<SolrQuery.SortClause> defaultFieldSortOrderPairs =
            Utils.modifiableList(
                    singletonList(SolrQuery.SortClause.create(SCORE, SolrQuery.ORDER.desc)));

    public List<SolrQuery.SortClause> getSort(String sortClause) {
        return Utils.nullOrEmpty(sortClause) ? createDefaultSort() : createSort(sortClause);
    }

    protected abstract String getSolrDocumentIdFieldName();

    protected abstract UniProtDataType getUniProtDataType();

    protected void resetDefaultFieldOrderPairs() {
        this.defaultFieldSortOrderPairs.clear();
    }

    protected void addDefaultFieldOrderPair(String sortFieldName, SolrQuery.ORDER direction) {
        this.defaultFieldSortOrderPairs.add(SolrQuery.SortClause.create(sortFieldName, direction));
    }

    protected String getSolrSortFieldName(String searchFieldName) {
        UniProtDataType dataType = getUniProtDataType();
        SearchFieldConfig searchFieldConfig = getSearchFieldConfig(dataType);
        return searchFieldConfig.getCorrespondingSortField(searchFieldName).getFieldName();
    }

    protected List<SolrQuery.SortClause> createSort(String sortClause) {
        return parseSortClause(sortClause);
    }

    protected List<SolrQuery.SortClause> createDefaultSort() {
        return this.defaultFieldSortOrderPairs;
    }

    protected SearchFieldConfig getSearchFieldConfig(UniProtDataType dataType) {
        return SearchFieldConfigFactory.getSearchFieldConfig(dataType);
    }

    protected List<SolrQuery.SortClause> parseSortClause(String sortClause) {
        List<SolrQuery.SortClause> fieldSortPairs = new ArrayList<>();

        String[] tokenizedSortClause =
                sortClause.split("\\s*,\\s*"); // e.g. field1 asc, field2 desc, field3 asc
        boolean hasIdField = false;

        for (String singleSortPairStr : tokenizedSortClause) {
            String[] fieldSortPairArr = singleSortPairStr.split("\\s+");
            if (fieldSortPairArr.length != 2) {
                throw new IllegalArgumentException("You must pass the field and sort direction.");
            }
            String solrFieldName = getSolrSortFieldName(fieldSortPairArr[0]);

            fieldSortPairs.add(
                    SolrQuery.SortClause.create(
                            solrFieldName, SolrQuery.ORDER.valueOf(fieldSortPairArr[1])));
            if (solrFieldName.equals(getSolrDocumentIdFieldName())) {
                hasIdField = true;
            }
        }

        if (fieldSortPairs.isEmpty()) { // sort by default fields
            fieldSortPairs = this.defaultFieldSortOrderPairs;
        } else if (!hasIdField) {
            fieldSortPairs.add(
                    SolrQuery.SortClause.create(getSolrDocumentIdFieldName(), SolrQuery.ORDER.asc));
        }

        return fieldSortPairs;
    }
}
