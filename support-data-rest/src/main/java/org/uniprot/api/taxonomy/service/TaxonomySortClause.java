package org.uniprot.api.taxonomy.service;

import javax.annotation.PostConstruct;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

@Component
public class TaxonomySortClause extends AbstractSolrSortClause {
    private static final String DOC_ID = "id";
    private static final String TAX_ID = "tax_id";

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(TAX_ID, Sort.Direction.ASC);
        addDefaultFieldOrderPair(DOC_ID, Sort.Direction.ASC);
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
