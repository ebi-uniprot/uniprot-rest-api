package org.uniprot.api.uniparc.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
@Import(UniParcQueryBoostsConfig.class)
public class UniParcQueryService extends StoreStreamerSearchService<UniParcDocument, UniParcEntry> {
    private static final String UNIPARC_ID_FIELD = "upi";
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;

    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcSortClause solrSortClause,
            UniParcQueryResultConverter uniParcQueryResultConverter,
            StoreStreamer<UniParcEntry> storeStreamer,
            QueryBoosts uniParcQueryBoosts) {

        super(
                repository,
                uniParcQueryResultConverter,
                solrSortClause,
                facetConfig,
                storeStreamer,
                uniParcQueryBoosts);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        this.queryProcessor = new UniProtQueryProcessor(getDefaultSearchOptimisedFieldItems());    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIPARC_ID_FIELD);
    }

    @Override
    public UniParcEntry findByUniqueId(String uniqueId, String filters) {
        return findByUniqueId(uniqueId);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }
}
