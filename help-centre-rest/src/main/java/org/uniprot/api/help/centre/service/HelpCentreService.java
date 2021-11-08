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
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Service
@Import(HelpCentreSolrQueryConfig.class)
public class HelpCentreService extends BasicSearchService<HelpDocument, HelpCentreEntry> {

    static final String HELP_CENTRE_ID_FIELD =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.HELP)
                    .getSearchFieldItemByName("id")
                    .getFieldName();
    private final SearchFieldConfig searchFieldConfig;
    private final UniProtQueryProcessorConfig helpCentreQueryProcessorConfig;

    public HelpCentreService(
            SolrQueryRepository<HelpDocument> repository,
            Function<HelpDocument, HelpCentreEntry> entryConverter,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig helpCentreSolrQueryConf,
            HelpCentreFacetConfig facetConfig,
            SearchFieldConfig helpCentreSearchFieldConfig,
            UniProtQueryProcessorConfig helpCentreQueryProcessorConfig) {
        super(repository, entryConverter, solrSortClause, helpCentreSolrQueryConf, facetConfig);
        this.searchFieldConfig = helpCentreSearchFieldConfig;
        this.helpCentreQueryProcessorConfig = helpCentreQueryProcessorConfig;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(HELP_CENTRE_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return helpCentreQueryProcessorConfig;
    }

}
