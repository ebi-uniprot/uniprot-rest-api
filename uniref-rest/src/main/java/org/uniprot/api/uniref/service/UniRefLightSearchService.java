package org.uniprot.api.uniref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
@Import(UniRefQueryBoostsConfig.class)
public class UniRefLightSearchService
        extends StoreStreamerSearchService<UniRefDocument, UniRefEntryLight> {

    private final SearchFieldConfig searchFieldConfig;

    @Autowired
    public UniRefLightSearchService(
            UniRefQueryRepository repository,
            UniRefFacetConfig facetConfig,
            UniRefSortClause uniRefSortClause,
            UniRefQueryResultConverter uniRefQueryResultConverter,
            StoreStreamer<UniRefEntryLight> storeStreamer,
            QueryBoosts uniRefQueryBoosts) {
        super(
                repository,
                uniRefQueryResultConverter,
                uniRefSortClause,
                facetConfig,
                storeStreamer,
                uniRefQueryBoosts);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF);
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId, String fields) {
        return findByUniqueId(uniqueId);
    }

    @Override
    protected String getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId) {
        throw new UnsupportedOperationException(
                "UniRefLightSearchService does not support findByUniqueId, try to use UniRefEntryService");
    }

    @Override
    public UniRefEntryLight getEntity(String idField, String value) {
        throw new UnsupportedOperationException(
                "UniRefLightSearchService does not support getEntity, try to use UniRefEntryService");
    }
}
