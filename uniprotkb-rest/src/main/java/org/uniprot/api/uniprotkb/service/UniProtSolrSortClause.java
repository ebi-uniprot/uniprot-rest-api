package org.uniprot.api.uniprotkb.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

@Component
public class UniProtSolrSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, SCORE)
                .and(
                        new Sort(
                                        Sort.Direction.DESC,
                                        getSearchFieldConfig(getUniProtDataType())
                                                .getCorrespondingSortField("annotation_score")
                                                .getFieldName())
                                .and(
                                        new Sort(
                                                Sort.Direction.ASC,
                                                getSearchFieldConfig(getUniProtDataType())
                                                        .getCorrespondingSortField("accession")
                                                        .getFieldName())));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return getSearchFieldConfig(getUniProtDataType())
                .getCorrespondingSortField("accession")
                .getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return getSearchFieldConfig(getUniProtDataType())
                .getCorrespondingSortField(name)
                .getFieldName();
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }
}
