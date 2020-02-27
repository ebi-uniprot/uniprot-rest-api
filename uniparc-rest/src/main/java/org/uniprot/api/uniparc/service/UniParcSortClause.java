package org.uniprot.api.uniparc.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Component
public class UniParcSortClause extends AbstractSolrSortClause {

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC,
                getSearchFieldConfig(getUniProtDataType())
                        .getCorrespondingSortField("upi")
                        .getFieldName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return getSearchFieldConfig(getUniProtDataType())
                .getSearchFieldItemByName("upi")
                .getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.uniparc;
    }
}
