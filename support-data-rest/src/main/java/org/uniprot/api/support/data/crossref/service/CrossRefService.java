package org.uniprot.api.support.data.crossref.service;

import java.util.stream.Stream;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.support.data.crossref.repository.CrossRefRepository;
import org.uniprot.api.support.data.crossref.request.CrossRefEntryConverter;
import org.uniprot.api.support.data.crossref.request.CrossRefFacetConfig;
import org.uniprot.api.support.data.crossref.request.CrossRefSolrQueryConfig;
import org.uniprot.api.support.data.crossref.request.CrossRefSolrSortClause;
import org.uniprot.api.support.data.crossref.request.CrossRefStreamRequest;
import org.uniprot.core.cv.xdb.CrossRefEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@Service
@Import(CrossRefSolrQueryConfig.class)
public class CrossRefService extends BasicSearchService<CrossRefDocument, CrossRefEntry> {
    public static final String CROSS_REF_ID_FIELD = "id";
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;
    private final RDFStreamer rdfStreamer;
    private final SolrQueryConfig solrQueryConfig;

    public CrossRefService(
            CrossRefRepository crossRefRepository,
            CrossRefEntryConverter toCrossRefEntryConverter,
            CrossRefSolrSortClause crossRefSolrSortClause,
            CrossRefFacetConfig crossRefFacetConfig,
            SolrQueryConfig crossRefSolrQueryConf,
            QueryProcessor crossRefQueryProcessor,
            SearchFieldConfig crossRefSearchFieldConfig,
            RDFStreamer xrefRDFStreamer) {
        super(
                crossRefRepository,
                toCrossRefEntryConverter,
                crossRefSolrSortClause,
                crossRefSolrQueryConf,
                crossRefFacetConfig);
        this.searchFieldConfig = crossRefSearchFieldConfig;
        this.queryProcessor = crossRefQueryProcessor;
        this.rdfStreamer = xrefRDFStreamer;
        this.solrQueryConfig = crossRefSolrQueryConf;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(CROSS_REF_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    public Stream<String> streamRDF(CrossRefStreamRequest streamRequest) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig)
                        .rows(getDefaultBatchSize())
                        .totalRows(Integer.MAX_VALUE)
                        .build();
        return this.rdfStreamer.idsToRDFStoreStream(solrRequest);
    }
}
