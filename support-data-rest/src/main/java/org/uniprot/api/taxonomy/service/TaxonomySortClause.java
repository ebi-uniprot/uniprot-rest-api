package org.uniprot.api.taxonomy.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.UniProtSearchFields;

@Component
public class TaxonomySortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                UniProtSearchFields.TAXONOMY.getField("tax_id").getName()))
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                UniProtSearchFields.TAXONOMY.getField("id").getName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.TAXONOMY.getField("id").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return UniProtSearchFields.TAXONOMY.getSortFieldFor(name).getName();
    }
}
