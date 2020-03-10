package org.uniprot.api.proteome.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 17 May 2019
 */
@Component
public class GeneCentricSortClause extends AbstractSolrSortClause {
    private static final String ACCESSION_ID = "accession_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(ACCESSION_ID, Sort.Direction.ASC);
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
                                                        .getCorrespondingSortField(ACCESSION_ID)
                                                        .getFieldName()))) {
            return fieldSortPairs;
        } else {
            List<Pair<String, Sort.Direction>> newFieldSortPairs = new ArrayList<>();
            newFieldSortPairs.addAll(fieldSortPairs);
            newFieldSortPairs.add(
                    new ImmutablePair<>(
                            getSearchFieldConfig(getUniProtDataType())
                                    .getCorrespondingSortField(ACCESSION_ID)
                                    .getFieldName(),
                            Sort.Direction.ASC));
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
