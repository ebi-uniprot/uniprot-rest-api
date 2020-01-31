package org.uniprot.api.subcell.service;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.subcell.SubcellularLocationRepository;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
@Import(SubcellularLocationQueryBoostsConfig.class)
public class SubcellularLocationService
        extends BasicSearchService<SubcellularLocationDocument, SubcellularLocationEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.SUBCELL, "content", "id", emptyList());

    public SubcellularLocationService(
            SubcellularLocationRepository repository,
            SubcellularLocationEntryConverter subcellularLocationEntryConverter,
            SubcellularLocationSortClause subcellularLocationSortClause,
            QueryBoosts subcellQueryBoosts) {
        super(
                repository,
                subcellularLocationEntryConverter,
                subcellularLocationSortClause,
                handlerSupplier.get(),
                subcellQueryBoosts,
                null);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.SUBCELL.getField("id").getName();
    }
}
