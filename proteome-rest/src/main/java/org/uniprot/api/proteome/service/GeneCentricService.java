package org.uniprot.api.proteome.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

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
        return UniProtSearchFields.GENECENTRIC.getField("accession_id").getName();
    }
}
