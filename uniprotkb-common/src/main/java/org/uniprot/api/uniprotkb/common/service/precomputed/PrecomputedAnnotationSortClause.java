package org.uniprot.api.uniprotkb.common.service.precomputed;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

@Component
public class PrecomputedAnnotationSortClause extends AbstractSolrSortClause {
    private static final String ACCESSION = "accession";

    @PostConstruct
    public void init() {
        resetDefaultFieldOrderPairs();
        addDefaultFieldOrderPair(ACCESSION, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return ACCESSION;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.PRECOMPUTED_ANNOTATION;
    }
}
