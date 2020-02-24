package org.uniprot.api.proteome.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */
@Component
public class ProteomeSortClause extends AbstractSolrSortClause {
    private static final String UPID = "upid";
    private SearchFieldConfig fieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.proteome);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                        Sort.Direction.DESC,
                        fieldConfig.getCorrespondingSortField("annotation_score").getFieldName())
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                fieldConfig.getCorrespondingSortField(UPID).getFieldName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return fieldConfig.getSearchFieldItemByName(UPID).getFieldName();
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
                                                fieldConfig
                                                        .getCorrespondingSortField(UPID)
                                                        .getFieldName()))) {
            return fieldSortPairs;
        } else {
            List<Pair<String, Sort.Direction>> newFieldSortPairs = new ArrayList<>();
            newFieldSortPairs.addAll(fieldSortPairs);
            newFieldSortPairs.add(
                    new ImmutablePair<>(
                            fieldConfig.getCorrespondingSortField(UPID).getFieldName(),
                            Sort.Direction.ASC));
            return newFieldSortPairs;
        }
    }
}
