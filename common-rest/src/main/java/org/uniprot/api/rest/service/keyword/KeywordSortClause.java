package org.uniprot.api.rest.service.keyword;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
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
        addDefaultFieldOrderPair(KEYWORD_ID, SolrQuery.ORDER.asc);
        addDefaultFieldOrderPair(DOC_ID, SolrQuery.ORDER.asc);
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
