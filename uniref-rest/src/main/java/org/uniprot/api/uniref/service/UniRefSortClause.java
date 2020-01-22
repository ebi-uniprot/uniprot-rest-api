package org.uniprot.api.uniref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefSortClause extends AbstractSolrSortClause {
    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC, UniProtSearchFields.UNIREF.getSortFieldFor("id").getName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.UNIREF.getField("id").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
