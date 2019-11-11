package org.uniprot.api.subcell.service;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.subcell.SubcellularLocationRepository;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;
import org.uniprot.store.search.field.SubcellularLocationField;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
public class SubcellularLocationService
        extends BasicSearchService<SubcellularLocationEntry, SubcellularLocationDocument> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            SubcellularLocationField.Search.content,
                            SubcellularLocationField.Search.id,
                            SubcellularLocationField.Search.getBoostFields());

    public SubcellularLocationService(
            SubcellularLocationRepository repository,
            SubcellularLocationEntryConverter subcellularLocationEntryConverter,
            SubcellularLocationSortClause subcellularLocationSortClause) {
        super(
                repository,
                subcellularLocationEntryConverter,
                subcellularLocationSortClause,
                handlerSupplier.get(),
                null);
    }

    @Override
    public SubcellularLocationEntry findByUniqueId(String uniqueId) {
        return getEntity(SubcellularLocationField.Search.id.name(), uniqueId);
    }
}
