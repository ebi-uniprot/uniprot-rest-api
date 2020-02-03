package org.uniprot.api.uniparc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
@Import(UniParcQueryBoostsConfig.class)
public class UniParcQueryService extends BasicSearchService<UniParcDocument, UniParcEntry> {
    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcEntryConverter uniParcEntryConverter,
            UniParcSortClause solrSortClause,
            QueryBoosts uniParcQueryBoosts) {

        super(repository, uniParcEntryConverter, solrSortClause, uniParcQueryBoosts, facetConfig);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.UNIPARC.getField("upi").getName();
    }
}
