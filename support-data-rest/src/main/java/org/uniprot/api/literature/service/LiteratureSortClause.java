package org.uniprot.api.literature.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.LiteratureField;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Component
public class LiteratureSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.ASC, LiteratureField.Search.id.name()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.LITERATURE.getField("id").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return LiteratureField.Sort.valueOf(name).getSolrFieldName();
    }
}
