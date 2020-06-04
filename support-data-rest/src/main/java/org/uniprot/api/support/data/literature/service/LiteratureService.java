package org.uniprot.api.support.data.literature.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.support.data.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Service
@Import(LiteratureQueryBoostsConfig.class)
public class LiteratureService extends BasicSearchService<LiteratureDocument, LiteratureEntry> {
    private SearchFieldConfig searchFieldConfig;

    public LiteratureService(
            LiteratureRepository repository,
            LiteratureEntryConverter entryConverter,
            LiteratureFacetConfig facetConfig,
            LiteratureSortClause literatureSortClause,
            QueryBoosts literatureQueryBoosts) {
        super(repository, entryConverter, literatureSortClause, literatureQueryBoosts, facetConfig);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.LITERATURE);
    }

    @Override
    protected String getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }
}
