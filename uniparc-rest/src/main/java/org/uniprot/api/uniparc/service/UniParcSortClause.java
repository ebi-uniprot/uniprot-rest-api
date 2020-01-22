package org.uniprot.api.uniparc.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Component
public class UniParcSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC, UniProtSearchFields.UNIPARC.getSortFieldFor("upi").getName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.UNIPARC.getField("upi").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
