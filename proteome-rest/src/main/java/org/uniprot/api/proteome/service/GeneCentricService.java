package org.uniprot.api.proteome.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.DefaultSearchQueryOptimiser;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
@Import(GeneCentricQueryBoostsConfig.class)
public class GeneCentricService extends BasicSearchService<GeneCentricDocument, CanonicalProtein> {
    private static final String GENECENTRIC_ID_FIELD = "accession_id";
    private final SearchFieldConfig searchFieldConfig;
    private final DefaultSearchQueryOptimiser defaultSearchQueryOptimiser;

    @Autowired
    public GeneCentricService(
            GeneCentricQueryRepository repository,
            GeneCentricFacetConfig facetConfig,
            GeneCentricSortClause solrSortClause,
            QueryBoosts geneCentricQueryBoosts) {
        super(
                repository,
                new GeneCentricEntryConverter(),
                solrSortClause,
                geneCentricQueryBoosts,
                facetConfig);
        searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.GENECENTRIC);
        this.defaultSearchQueryOptimiser =
                new DefaultSearchQueryOptimiser(getDefaultSearchOptimisedFieldItems());
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(GENECENTRIC_ID_FIELD);
    }

    @Override
    protected DefaultSearchQueryOptimiser getDefaultSearchQueryOptimiser() {
        return defaultSearchQueryOptimiser;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }
}
