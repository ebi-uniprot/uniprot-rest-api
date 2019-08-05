package org.uniprot.api.crossref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.CrossRefField;

@Service
public class CrossRefSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort = new Sort(Sort.Direction.ASC, CrossRefField.Search.accession.getName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return CrossRefField.Search.accession.getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }

}