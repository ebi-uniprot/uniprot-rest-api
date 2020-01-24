package org.uniprot.api.crossref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.UniProtSearchFields;

@Component
public class CrossRefSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort =
                new Sort(
                        Sort.Direction.ASC,
                        UniProtSearchFields.CROSSREF.getField("accession").getName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtSearchFields.CROSSREF.getField("accession").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
