package org.uniprot.api.literature.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Component
public class LiteratureSortClause extends AbstractSolrSortClause {
    private SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.literature);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, "score")
                .and(
                        new Sort(
                                Sort.Direction.ASC,
                                this.searchFieldConfig
                                        .getSearchFieldItemByName("id")
                                        .getFieldName()));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return this.searchFieldConfig.getCorrespondingSortField(name).getFieldName();
    }
}
