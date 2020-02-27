package org.uniprot.api.literature.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Component
public class LiteratureSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                getSearchFieldConfig(getUniProtDataType())
                                        .getSearchFieldItemByName("id")
                                        .getFieldName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return getSearchFieldConfig(getUniProtDataType())
                .getSearchFieldItemByName("id")
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
        return UniProtDataType.literature;
    }
}
