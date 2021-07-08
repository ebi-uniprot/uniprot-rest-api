package org.uniprot.api.help.centre.service;

import java.util.function.Function;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.help.centre.repository.HelpCentreFacetConfig;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Service
@Import(HelpCentreSolrQueryConfig.class)
public class HelperCentreService extends BasicSearchService<HelpDocument, HelpCentreEntry> {

    private final SearchFieldConfig searchFieldConfig;
    static final String HELP_CENTRE_ID_FIELD = "id";

    public HelperCentreService(
            SolrQueryRepository<HelpDocument> repository,
            Function<HelpDocument, HelpCentreEntry> entryConverter,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig helpCentreSolrQueryConf,
            HelpCentreFacetConfig facetConfig,
            SearchFieldConfig helpCentreSearchFieldConfig) {
        super(repository, entryConverter, solrSortClause, helpCentreSolrQueryConf, facetConfig);
        this.searchFieldConfig = helpCentreSearchFieldConfig;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(HELP_CENTRE_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return null;
    }
}
