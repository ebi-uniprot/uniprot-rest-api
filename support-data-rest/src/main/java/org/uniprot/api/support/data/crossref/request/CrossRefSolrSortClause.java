package org.uniprot.api.support.data.crossref.request;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

@Component
public class CrossRefSolrSortClause extends AbstractSolrSortClause {
    private static final String ABBREVIATION = "abbrev";
    private static final String DOC_ID = "id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(ABBREVIATION, SolrQuery.ORDER.asc);
        addDefaultFieldOrderPair(DOC_ID, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DOC_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.CROSSREF;
    }
}
