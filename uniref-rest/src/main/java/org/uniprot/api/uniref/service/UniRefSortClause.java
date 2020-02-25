package org.uniprot.api.uniref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Component
public class UniRefSortClause extends AbstractSolrSortClause {
    private SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.uniref);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(
                Sort.Direction.ASC,
                searchFieldConfig.getCorrespondingSortField("id").getFieldName());
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
