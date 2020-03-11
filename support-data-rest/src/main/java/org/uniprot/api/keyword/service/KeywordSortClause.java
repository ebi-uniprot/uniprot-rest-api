package org.uniprot.api.keyword.service;

import javax.annotation.PostConstruct;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/** @author lgonzales */
@Component
public class KeywordSortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "id";
    private static final String KEYWORD_ID = "keyword_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(KEYWORD_ID, Sort.Direction.ASC);
        addDefaultFieldOrderPair(DOC_ID, Sort.Direction.ASC);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DOC_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.KEYWORD;
    }
}
