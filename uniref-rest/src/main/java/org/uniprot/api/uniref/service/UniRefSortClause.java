package org.uniprot.api.uniref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.UniRefField;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefSortClause extends AbstractSolrSortClause {
    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.ASC, UniRefField.Sort.id.getSolrFieldName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniRefField.Search.id.name();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
