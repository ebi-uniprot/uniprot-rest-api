package org.uniprot.api.keyword.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.KeywordField;

/** @author lgonzales */
@Component
public class KeywordSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.ASC, KeywordField.Search.keyword_id.name()))
                .and(new Sort(Sort.Direction.ASC, KeywordField.Search.id.name()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.KEYWORD.getField("id").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return KeywordField.Sort.valueOf(name).getSolrFieldName();
    }
}
