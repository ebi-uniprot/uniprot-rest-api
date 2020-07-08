package org.uniprot.api.proteome.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 17 May 2019
 */
@Component
public class GeneCentricSortClause extends AbstractSolrSortClause {
    private static final String ACCESSION_ID = "accession_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(ACCESSION_ID, SolrQuery.ORDER.asc);
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
                                                        .getCorrespondingSortField(ACCESSION_ID)
                                                        .getFieldName()))) {
            return fieldSortPairs;
        } else {
            List<SolrQuery.SortClause> newFieldSortPairs = new ArrayList<>(fieldSortPairs);
            newFieldSortPairs.add(
                    SolrQuery.SortClause.create(
                            getSearchFieldConfig(getUniProtDataType())
                                    .getCorrespondingSortField(ACCESSION_ID)
                                    .getFieldName(),
                            SolrQuery.ORDER.asc));
            return newFieldSortPairs;
        }
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return ACCESSION_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.GENECENTRIC;
    }
}
