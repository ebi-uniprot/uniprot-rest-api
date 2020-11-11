package org.uniprot.api.unirule.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

import javax.annotation.PostConstruct;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Component
public class UniRuleSortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "unirule_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(DOC_ID, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DOC_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIRULE;
    }
}
