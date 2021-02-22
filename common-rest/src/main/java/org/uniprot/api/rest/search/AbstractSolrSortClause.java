package org.uniprot.api.rest.search;

import static java.util.Collections.singletonList;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

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

    protected List<SolrQuery.SortClause> createSort(String sortClause) {
        return parseSortClause(sortClause);
    }

    protected List<SolrQuery.SortClause> createDefaultSort() {
        return this.defaultFieldSortOrderPairs;
    }

    protected List<SolrQuery.SortClause> parseSortClause(String sortClause) {
        List<SolrQuery.SortClause> fieldSortPairs =
                SortUtils.parseSortClause(getUniProtDataType(), sortClause);

        boolean hasIdField =
                fieldSortPairs.stream()
                        .anyMatch(
                                sortItem ->
                                        sortItem.getItem().equals(getSolrDocumentIdFieldName()));

        if (fieldSortPairs.isEmpty()) { // sort by default fields
            fieldSortPairs = this.defaultFieldSortOrderPairs;
        } else if (!hasIdField) {
            fieldSortPairs.add(
                    SolrQuery.SortClause.create(getSolrDocumentIdFieldName(), SolrQuery.ORDER.asc));
        }

        return fieldSortPairs;
    }
}
