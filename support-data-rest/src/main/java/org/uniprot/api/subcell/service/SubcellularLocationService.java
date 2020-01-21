package org.uniprot.api.subcell.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.subcell.SubcellularLocationRepository;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;
import org.uniprot.store.search.domain2.UniProtSearchFields;
import org.uniprot.store.search.field.SubcellularLocationField;

import java.util.function.Supplier;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
public class SubcellularLocationService
        extends BasicSearchService<SubcellularLocationDocument, SubcellularLocationEntry> {

    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.SUBCELL,
                            "content",
                            "id",
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
    protected String getIdField() {
        return SubcellularLocationField.Search.id.name();
    }
}
