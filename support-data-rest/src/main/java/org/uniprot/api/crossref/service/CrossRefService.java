package org.uniprot.api.crossref.service;

import static java.util.Collections.emptyList;

import java.util.function.Supplier;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.crossref.config.CrossRefFacetConfig;
import org.uniprot.api.crossref.config.CrossRefQueryBoostsConfig;
import org.uniprot.api.crossref.repository.CrossRefRepository;
import org.uniprot.api.crossref.request.CrossRefEntryConverter;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.crossref.CrossRefEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;
import org.uniprot.store.search.field.UniProtSearchFields;

@Service
@Import(CrossRefQueryBoostsConfig.class)
public class CrossRefService extends BasicSearchService<CrossRefDocument, CrossRefEntry> {
    private static Supplier<DefaultSearchHandler> handlerSupplier =
            () ->
                    new DefaultSearchHandler(
                            UniProtSearchFields.CROSSREF, "content", "accession", emptyList());

    public CrossRefService(
            CrossRefRepository crossRefRepository,
            CrossRefEntryConverter toCrossRefEntryConverter,
            CrossRefSolrSortClause crossRefSolrSortClause,
            CrossRefFacetConfig crossRefFacetConfig,
            QueryBoosts crossRefQueryBoosts) {
        super(
                crossRefRepository,
                toCrossRefEntryConverter,
                crossRefSolrSortClause,
                handlerSupplier.get(),
                crossRefQueryBoosts,
                crossRefFacetConfig);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.CROSSREF.getField("accession").getName();
    }
}
