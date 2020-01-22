package org.uniprot.api.keyword.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;

/** @author lgonzales */
@Component
public class KeywordSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                UniProtSearchFields.KEYWORD.getField("keyword_id").getName()))
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                UniProtSearchFields.KEYWORD.getField("id").getName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.KEYWORD.getField("id").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return UniProtSearchFields.KEYWORD.getSortFieldFor(name).getName();
    }
}
