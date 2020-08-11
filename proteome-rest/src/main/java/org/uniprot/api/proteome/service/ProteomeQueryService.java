package org.uniprot.api.proteome.service;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.DefaultSearchQueryOptimiser;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
@Import(ProteomeQueryBoostsConfig.class)
public class ProteomeQueryService extends BasicSearchService<ProteomeDocument, ProteomeEntry> {
    private static final String PROTEOME_ID_FIELD = "upid";
    private final SearchFieldConfig fieldConfig;
    private final DefaultSearchQueryOptimiser defaultSearchQueryOptimiser;

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
        fieldConfig = SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PROTEOME);
        this.defaultSearchQueryOptimiser =
                new DefaultSearchQueryOptimiser(getDefaultSearchOptimisedFieldItems());
    }

    @Override
    protected SearchFieldItem getIdField() {
        return fieldConfig.getSearchFieldItemByName(PROTEOME_ID_FIELD);
    }

    @Override
    protected DefaultSearchQueryOptimiser getDefaultSearchQueryOptimiser() {
        return defaultSearchQueryOptimiser;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }
}
