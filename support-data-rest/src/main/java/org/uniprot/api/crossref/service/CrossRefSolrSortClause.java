package org.uniprot.api.crossref.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

@Component
public class CrossRefSolrSortClause extends AbstractSolrSortClause {
    private SearchFieldConfig fieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.crossref);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        Sort defaultSort =
                new Sort(
                        Sort.Direction.ASC,
                        fieldConfig.getSearchFieldItemByName("accession").getFieldName());

        if (hasScore) {
            defaultSort = new Sort(Sort.Direction.DESC, "score").and(defaultSort);
        }

        return defaultSort;
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return fieldConfig.getSearchFieldItemByName("accession").getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return name;
    }
}
