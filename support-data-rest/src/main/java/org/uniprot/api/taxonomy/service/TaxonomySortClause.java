package org.uniprot.api.taxonomy.service;

import org.springframework.data.domain.Sort;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.TaxonomyField;

public class TaxonomySortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(new Sort(Sort.Direction.ASC, TaxonomyField.Search.tax_id.name()))
                .and(new Sort(Sort.Direction.ASC, TaxonomyField.Search.id.name()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return TaxonomyField.Search.id.getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return TaxonomyField.Sort.valueOf(name).getSolrFieldName();
    }

}
