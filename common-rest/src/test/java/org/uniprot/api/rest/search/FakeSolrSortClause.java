package org.uniprot.api.rest.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author lgonzales
 * @since 01/10/2020
 */
public class FakeSolrSortClause extends AbstractSolrSortClause {
    public static final String ID = "id_field";

    public FakeSolrSortClause() {
        addDefaultFieldOrderPair("default", SolrQuery.ORDER.asc);
        addDefaultFieldOrderPair(FakeSolrSortClause.ID, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return ID;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return null;
    }
}
