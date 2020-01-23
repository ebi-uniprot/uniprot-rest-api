package org.uniprot.api.proteome.service;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.CanonicalProtein;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;
import org.uniprot.store.search.field.UniProtSearchFields;
import org.uniprot.store.search.field.GeneCentricField.Search;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
public class GeneCentricService extends BasicSearchService<GeneCentricDocument, CanonicalProtein> {
    private static final Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.GENECENTRIC,
                            "accession",
                            "accession_id",
                            Search.getBoostFields());

    @Autowired
    public GeneCentricService(
            GeneCentricQueryRepository repository,
            GeneCentricFacetConfig facetConfig,
            GeneCentricSortClause solrSortClause) {
        super(
                repository,
                new GeneCentricEntryConverter(),
                solrSortClause,
                handlerSupplier.get(),
                facetConfig);
    }

    @Override
    protected String getIdField() {
        return Search.accession_id.name();
    }
}
