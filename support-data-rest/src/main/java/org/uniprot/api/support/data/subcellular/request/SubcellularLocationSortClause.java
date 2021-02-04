package org.uniprot.api.support.data.subcellular.request;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Component
public class SubcellularLocationSortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "id";

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
        return UniProtDataType.SUBCELLLOCATION;
    }
}
