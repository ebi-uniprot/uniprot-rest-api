package org.uniprot.api.support.data.subcell.service;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.DefaultSearchQueryOptimiser;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.api.support.data.subcell.SubcellularLocationRepository;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
@Import(SubcellularLocationQueryBoostsConfig.class)
public class SubcellularLocationService
        extends BasicSearchService<SubcellularLocationDocument, SubcellularLocationEntry> {
    private static final String SUBCELL_ID_FIELD = "id";
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;

    public SubcellularLocationService(
            SubcellularLocationRepository repository,
            SubcellularLocationEntryConverter subcellularLocationEntryConverter,
            SubcellularLocationSortClause subcellularLocationSortClause,
            QueryBoosts subcellQueryBoosts) {
        super(
                repository,
                subcellularLocationEntryConverter,
                subcellularLocationSortClause,
                subcellQueryBoosts,
                null);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.SUBCELLLOCATION);
        this.queryProcessor =
                UniProtQueryProcessor.builder()
                        .queryProcessorPipeline(
                                new UniProtQueryNodeProcessorPipeline(
                                        getDefaultSearchOptimisedFieldItems()))
                        .build();;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(SUBCELL_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }
}
