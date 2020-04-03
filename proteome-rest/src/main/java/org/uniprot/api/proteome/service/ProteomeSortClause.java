package org.uniprot.api.proteome.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
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
        addDefaultFieldOrderPair(ANNOTATION_SCORE, Sort.Direction.DESC);
        addDefaultFieldOrderPair(UPID, Sort.Direction.ASC);
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
    protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
        List<Pair<String, Sort.Direction>> fieldSortPairs = super.parseSortClause(sortClause);
        if (fieldSortPairs.stream()
                .anyMatch(
                        val ->
                                val.getLeft()
                                        .equals(
                                                getSearchFieldConfig(getUniProtDataType())
                                                        .getCorrespondingSortField(UPID)
                                                        .getFieldName()))) {
            return fieldSortPairs;
        } else {
            List<Pair<String, Sort.Direction>> newFieldSortPairs = new ArrayList<>();
            newFieldSortPairs.addAll(fieldSortPairs);
            newFieldSortPairs.add(
                    new ImmutablePair<>(
                            getSearchFieldConfig(getUniProtDataType())
                                    .getCorrespondingSortField(UPID)
                                    .getFieldName(),
                            Sort.Direction.ASC));
            return newFieldSortPairs;
        }
    }
}
