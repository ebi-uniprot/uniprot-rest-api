package org.uniprot.api.support.data.crossref.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.CROSS_REF_REGEX;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.crossref.repository.CrossRefRepository;
import org.uniprot.api.support.data.crossref.request.*;
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
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<CrossRefDocument> documentIdStream;
    private static final Pattern CROSS_REF_REGEX_PATTERN = Pattern.compile(CROSS_REF_REGEX);

    public CrossRefService(
            CrossRefRepository crossRefRepository,
            CrossRefEntryConverter toCrossRefEntryConverter,
            CrossRefSolrSortClause crossRefSolrSortClause,
            CrossRefFacetConfig crossRefFacetConfig,
            SolrQueryConfig crossRefSolrQueryConf,
            UniProtQueryProcessorConfig crossRefQueryProcessorConfig,
            SearchFieldConfig crossRefSearchFieldConfig,
            DefaultDocumentIdStream<CrossRefDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer) {
        super(
                crossRefRepository,
                toCrossRefEntryConverter,
                crossRefSolrSortClause,
                crossRefSolrQueryConf,
                crossRefFacetConfig);
        this.crossRefQueryProcessorConfig = crossRefQueryProcessorConfig;
        this.searchFieldConfig = crossRefSearchFieldConfig;
        this.documentIdStream = documentIdStream;
        this.rdfStreamer = supportDataRdfStreamer;
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
    protected DefaultDocumentIdStream<CrossRefDocument> getDocumentIdStream() {
        return documentIdStream;
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        CrossRefSearchRequest xrefRequest = (CrossRefSearchRequest) request;
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
        if (CROSS_REF_REGEX_PATTERN.matcher(cleanQuery.toUpperCase()).matches()) {
            xrefRequest.setQuery(cleanQuery.toUpperCase());
        }
        return super.createSearchSolrRequest(xrefRequest);
    }
}
