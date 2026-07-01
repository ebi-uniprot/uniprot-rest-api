package org.uniprot.api.aa.service;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Component
public class ArbaSortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "rule_id";

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
        return UniProtDataType.ARBA;
    }
}
