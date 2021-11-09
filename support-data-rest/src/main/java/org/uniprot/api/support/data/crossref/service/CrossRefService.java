package org.uniprot.api.support.data.crossref.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.crossref.repository.CrossRefRepository;
import org.uniprot.api.support.data.crossref.request.CrossRefEntryConverter;
import org.uniprot.api.support.data.crossref.request.CrossRefFacetConfig;
import org.uniprot.api.support.data.crossref.request.CrossRefSolrQueryConfig;
import org.uniprot.api.support.data.crossref.request.CrossRefSolrSortClause;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@Service
@Import(CrossRefSolrQueryConfig.class)
public class CrossRefService extends BasicSearchService<CrossRefDocument, CrossRefEntry> {
    public static final String CROSS_REF_ID_FIELD = "id";
    private final UniProtQueryProcessorConfig crossRefQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final RDFStreamer rdfStreamer;

    public CrossRefService(
            CrossRefRepository crossRefRepository,
            CrossRefEntryConverter toCrossRefEntryConverter,
            CrossRefSolrSortClause crossRefSolrSortClause,
            CrossRefFacetConfig crossRefFacetConfig,
            SolrQueryConfig crossRefSolrQueryConf,
            UniProtQueryProcessorConfig crossRefQueryProcessorConfig,
            SearchFieldConfig crossRefSearchFieldConfig,
            @Qualifier("xrefRDFStreamer") RDFStreamer xrefRDFStreamer) {
        super(
                crossRefRepository,
                toCrossRefEntryConverter,
                crossRefSolrSortClause,
                crossRefSolrQueryConf,
                crossRefFacetConfig);
        this.crossRefQueryProcessorConfig = crossRefQueryProcessorConfig;
        this.searchFieldConfig = crossRefSearchFieldConfig;
        this.rdfStreamer = xrefRDFStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(CROSS_REF_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return crossRefQueryProcessorConfig;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return this.rdfStreamer;
    }
}
