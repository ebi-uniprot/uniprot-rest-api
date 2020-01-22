package org.uniprot.api.proteome.service;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jluo
 * @date: 17 May 2019
 */
@Component
public class GeneCentricSortClause extends AbstractSolrSortClause {
    private static final String ACCESSION_ID = "accession_id";

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC,
                UniProtSearchFields.GENECENTRIC.getSortFieldFor(ACCESSION_ID).getName());
    }

    @Override
    protected List<Pair<String, Sort.Direction>> parseSortClause(String sortClause) {
        List<Pair<String, Sort.Direction>> fieldSortPairs = super.parseSortClause(sortClause);
        if (fieldSortPairs.stream()
                .anyMatch(
                        val ->
                                val.getLeft()
                                        .equals(
                                                UniProtSearchFields.GENECENTRIC
                                                        .getSortFieldFor(ACCESSION_ID)
                                                        .getName()))) {
            return fieldSortPairs;
        } else {
            List<Pair<String, Sort.Direction>> newFieldSortPairs = new ArrayList<>();
            newFieldSortPairs.addAll(fieldSortPairs);
            newFieldSortPairs.add(
                    new ImmutablePair<>(
                            UniProtSearchFields.GENECENTRIC
                                    .getSortFieldFor(ACCESSION_ID)
                                    .getName(),
                            Sort.Direction.ASC));
            return newFieldSortPairs;
        }
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.GENECENTRIC.getField(ACCESSION_ID).getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
