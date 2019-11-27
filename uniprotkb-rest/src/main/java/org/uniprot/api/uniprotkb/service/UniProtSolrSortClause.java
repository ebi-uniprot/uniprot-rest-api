package org.uniprot.api.uniprotkb.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.domain2.UniProtKBSearchFields;

@Component
public class UniProtSolrSortClause extends AbstractSolrSortClause {
    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, SCORE)
                .and(
                        new Sort(
                                        Sort.Direction.DESC,
                                        UniProtKBSearchFields.INSTANCE.getSortFieldFor(
                                                "annotation_score").getName())
                                .and(
                                        new Sort(
                                                Sort.Direction.ASC,
                                                UniProtKBSearchFields.INSTANCE.getSortFieldFor(
                                                        "accession").getName())));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtKBSearchFields.INSTANCE.getSortFieldFor("accession").getName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return UniProtKBSearchFields.INSTANCE.getSortFieldFor(name).getName();
    }
}
