package org.uniprot.api.subcell.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.subcell.SubcellularLocationRepository;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
@Import(SubcellularLocationQueryBoostsConfig.class)
public class SubcellularLocationService
        extends BasicSearchService<SubcellularLocationDocument, SubcellularLocationEntry> {
    private SearchFieldConfig searchFieldConfig;

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
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.subcelllocation);
    }

    @Override
    protected String getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id").getFieldName();
    }
}
