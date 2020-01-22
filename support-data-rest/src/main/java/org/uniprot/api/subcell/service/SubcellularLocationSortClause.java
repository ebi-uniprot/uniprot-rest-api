package org.uniprot.api.subcell.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Component
public class SubcellularLocationSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.ASC, SubcellularLocationField.Search.id.name()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.SUBCELL.getField("id").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return SubcellularLocationField.Sort.valueOf(name).getSolrFieldName();
    }
}
