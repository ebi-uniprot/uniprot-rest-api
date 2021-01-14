package org.uniprot.api.support.data.taxonomy.request;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

@Component
public class TaxonomySortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "id";
    private static final String TAX_ID = "tax_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(TAX_ID, SolrQuery.ORDER.asc);
        addDefaultFieldOrderPair(DOC_ID, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return DOC_ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.TAXONOMY;
    }
}
