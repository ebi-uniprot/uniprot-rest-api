package org.uniprot.api.proteome.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */
@Component
public class ProteomeSortClause extends AbstractSolrSortClause {
    private static final String UPID = "upid";

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                        Sort.Direction.DESC,
                        UniProtSearchFields.PROTEOME.getSortFieldFor("annotation_score").getName())
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                UniProtSearchFields.PROTEOME.getSortFieldFor(UPID).getName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.PROTEOME.getField(UPID).getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }

    @Override
    protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
        List<Pair<String, Sort.Direction>> fieldSortPairs = super.parseSortClause(sortClause);
        if (fieldSortPairs.stream()
                .anyMatch(
                        val ->
                                val.getLeft()
                                        .equals(
                                                UniProtSearchFields.PROTEOME
                                                        .getSortFieldFor(UPID)
                                                        .getName()))) {
            return fieldSortPairs;
        } else {
            List<Pair<String, Sort.Direction>> newFieldSortPairs = new ArrayList<>();
            newFieldSortPairs.addAll(fieldSortPairs);
            newFieldSortPairs.add(
                    new ImmutablePair<>(
                            UniProtSearchFields.PROTEOME.getSortFieldFor(UPID).getName(),
                            Sort.Direction.ASC));
            return newFieldSortPairs;
        }
    }
}
