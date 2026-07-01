package org.uniprot.api.proteome.service;

import static org.uniprot.api.rest.search.SortUtils.*;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */
@Component
public class ProteomeSortClause extends AbstractSolrSortClause {
    private static final String UPID = "upid";
    private static final String ANNOTATION_SCORE = "annotation_score";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(ANNOTATION_SCORE, SolrQuery.ORDER.desc);
        addDefaultFieldOrderPair(UPID, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UPID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.PROTEOME;
    }

    @Override
    protected List<SolrQuery.SortClause> parseSortClause(String sortClauseRaw) {
        List<SolrQuery.SortClause> fieldSortPairs = super.parseSortClause(sortClauseRaw);

        if (fieldSortPairs.stream()
                .anyMatch(
                        clause ->
                                clause.getItem()
                                        .equals(
                                                getSearchFieldConfig(getUniProtDataType())
                                                        .getCorrespondingSortField(UPID)
                                                        .getFieldName()))) {
            return fieldSortPairs;
        } else {
            List<SolrQuery.SortClause> newFieldSortPairs = new ArrayList<>(fieldSortPairs);
            newFieldSortPairs.add(
                    SolrQuery.SortClause.create(
                            getSearchFieldConfig(getUniProtDataType())
                                    .getCorrespondingSortField(UPID)
                                    .getFieldName(),
                            SolrQuery.ORDER.asc));
            return newFieldSortPairs;
        }
    }
}
