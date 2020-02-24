package org.uniprot.api.proteome.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
@Import(ProteomeQueryBoostsConfig.class)
public class ProteomeQueryService extends BasicSearchService<ProteomeDocument, ProteomeEntry> {
    private SearchFieldConfig fieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.proteome);

    public ProteomeQueryService(
            ProteomeQueryRepository repository,
            ProteomeFacetConfig facetConfig,
            ProteomeSortClause solrSortClause,
            QueryBoosts proteomeQueryBoosts) {
        super(
                repository,
                new ProteomeEntryConverter(),
                solrSortClause,
                proteomeQueryBoosts,
                facetConfig);
    }

    @Override
    protected String getIdField() {
        return fieldConfig.getSearchFieldItemByName("upid").getFieldName();
    }
}
