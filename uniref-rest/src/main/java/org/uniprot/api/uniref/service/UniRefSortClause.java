package org.uniprot.api.uniref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC,
                getSearchFieldConfig(getUniProtDataType())
                        .getCorrespondingSortField("id")
                        .getFieldName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return getSearchFieldConfig(getUniProtDataType())
                .getSearchFieldItemByName("id")
                .getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.uniref;
    }
}
