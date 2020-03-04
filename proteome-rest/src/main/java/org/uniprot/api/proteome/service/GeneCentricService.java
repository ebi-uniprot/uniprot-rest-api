package org.uniprot.api.proteome.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
@Import(GeneCentricQueryBoostsConfig.class)
public class GeneCentricService extends BasicSearchService<GeneCentricDocument, CanonicalProtein> {
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
    }

    @Override
    protected String getIdField() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.GENECENTRIC);
        return fieldConfig.getSearchFieldItemByName("accession_id").getFieldName();
    }
}
