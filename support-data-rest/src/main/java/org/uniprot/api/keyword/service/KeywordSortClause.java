package org.uniprot.api.keyword.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/** @author lgonzales */
@Component
public class KeywordSortClause extends AbstractSolrSortClause {
    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                getSearchFieldConfig(getUniProtDataType())
                                        .getSearchFieldItemByName("keyword_id")
                                        .getFieldName()))
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                getSearchFieldConfig(getUniProtDataType())
                                        .getSearchFieldItemByName("id")
                                        .getFieldName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return getSearchFieldConfig(getUniProtDataType())
                .getSearchFieldItemByName("id")
                .getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return getSearchFieldConfig(getUniProtDataType())
                .getCorrespondingSortField(name)
                .getFieldName();
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.KEYWORD;
    }
}
