package org.uniprot.api.subcell.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
public class SubcellularLocationSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.ASC, SubcellularLocationField.Search.id.name()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return SubcellularLocationField.Search.id.getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return SubcellularLocationField.Sort.valueOf(name).getSolrFieldName();
    }
}
