package org.uniprot.api.uniprotkb.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;

@Component
public class UniProtSolrSortClause extends AbstractSolrSortClause {

    private SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.uniprotkb);

    @Override
    protected Sort createDefaultSort(boolean hasScore) {
        return new Sort(Sort.Direction.DESC, SCORE)
                .and(
                        new Sort(
                                        Sort.Direction.DESC,
                                        searchFieldConfig
                                                .getCorrespondingSortField("annotation_score")
                                                .getFieldName())
                                .and(
                                        new Sort(
                                                Sort.Direction.ASC,
                                                searchFieldConfig
                                                        .getCorrespondingSortField("accession")
                                                        .getFieldName())));
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return searchFieldConfig.getCorrespondingSortField("accession").getFieldName();
    }

    @Override
    protected String getSolrSortFieldName(String name) {
        return searchFieldConfig.getCorrespondingSortField(name).getFieldName();
    }
}
