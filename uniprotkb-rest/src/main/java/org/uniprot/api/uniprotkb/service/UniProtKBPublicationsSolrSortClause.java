package org.uniprot.api.uniprotkb.service;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

@Component
public class UniProtKBPublicationsSolrSortClause extends AbstractSolrSortClause {
    private static final String ID = "id";

    @PostConstruct
    public void init() {
        resetDefaultFieldOrderPairs();
        addDefaultFieldOrderPair(
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                        .getSearchFieldItemByName("main_type")
                        .getFieldName(),
                SolrQuery.ORDER.desc);
        addDefaultFieldOrderPair(
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                        .getSearchFieldItemByName("reference_number")
                        .getFieldName(),
                SolrQuery.ORDER.asc);
        addDefaultFieldOrderPair(
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION)
                        .getSearchFieldItemByName("pubmed_id")
                        .getFieldName(),
                SolrQuery.ORDER.desc);
        addDefaultFieldOrderPair("id", SolrQuery.ORDER.desc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return ID;
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
