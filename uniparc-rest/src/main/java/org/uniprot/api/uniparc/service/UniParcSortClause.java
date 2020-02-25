package org.uniprot.api.uniparc.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Component
public class UniParcSortClause extends AbstractSolrSortClause {

    private SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.uniparc);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC,
                searchFieldConfig.getCorrespondingSortField("upi").getFieldName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return searchFieldConfig.getSearchFieldItemByName("upi").getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
