package org.uniprot.api.uniprotkb.service;

import javax.annotation.PostConstruct;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

@Component
public class UniProtSolrSortClause extends AbstractSolrSortClause {
    private static final String ANNOTATION_SCORE = "annotation_score";
    private static final String ACCESSION_ID = "accession_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(ANNOTATION_SCORE, Sort.Direction.DESC);
        addDefaultFieldOrderPair(ACCESSION_ID, Sort.Direction.ASC);
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
