package org.uniprot.api.uniprotkb.service;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

@Component
public class UniProtSolrSortClause extends AbstractSolrSortClause {
    private static final String ANNOTATION_SCORE = "annotation_score";
    private static final String ACCESSION_ID = "accession_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(ANNOTATION_SCORE, SolrQuery.ORDER.desc);
        addDefaultFieldOrderPair(ACCESSION_ID, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return ACCESSION_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    public String getSolrSortFieldName(String name) {
        return super.getSolrSortFieldName(name);
    }
}
