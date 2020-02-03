package org.uniprot.api.literature.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.literature.repository.LiteratureFacetConfig;
import org.uniprot.api.literature.repository.LiteratureRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.store.search.document.literature.LiteratureDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Service
@Import(LiteratureQueryBoostsConfig.class)
public class LiteratureService extends BasicSearchService<LiteratureDocument, LiteratureEntry> {
    public LiteratureService(
            LiteratureRepository repository,
            LiteratureEntryConverter entryConverter,
            LiteratureFacetConfig facetConfig,
            LiteratureSortClause literatureSortClause,
            QueryBoosts literatureQueryBoosts) {
        super(repository, entryConverter, literatureSortClause, literatureQueryBoosts, facetConfig);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.LITERATURE.getField("id").getName();
    }
}
