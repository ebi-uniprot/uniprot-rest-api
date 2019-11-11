package org.uniprot.api.uniprotkb.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.field.UniProtField;

@Service
public class UniProtSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(
                        new Sort(
                                        Sort.Direction.DESC,
                                        UniProtField.Sort.annotation_score.getSolrFieldName())
                                .and(
                                        new Sort(
                                                Sort.Direction.ASC,
                                                UniProtField.Sort.accession.getSolrFieldName())));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return UniProtField.Sort.accession.getSolrFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return UniProtField.Sort.valueOf(name).getSolrFieldName();
    }
}
