package org.uniprot.api.support.data.common.keyword.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.KEYWORD_ID_REGEX;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.support.data.common.keyword.repository.KeywordFacetConfig;
import org.uniprot.api.support.data.common.keyword.repository.KeywordRepository;
import org.uniprot.api.support.data.common.keyword.request.KeywordBasicRequest;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.keyword.KeywordDocument;

@Service
@Import(KeywordSolrQueryConfig.class)
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {
    public static final String KEYWORD_ID_FIELD = "keyword_id";
    private final UniProtQueryProcessorConfig keywordQueryProcessorConfig;
    private final SearchFieldConfig fieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<KeywordDocument> documentIdStream;
    private static final Pattern KEYWORD_ID_REGEX_PATTERN = Pattern.compile(KEYWORD_ID_REGEX);

    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause,
            SolrQueryConfig keywordSolrQueryConf,
            UniProtQueryProcessorConfig keywordQueryProcessorConfig,
            KeywordFacetConfig facetConfig,
            SearchFieldConfig keywordSearchFieldConfig,
            DefaultDocumentIdStream<KeywordDocument> documentIdStream,
            RdfStreamer supportDataRdfStreamer) {
        super(
                repository,
                keywordEntryConverter,
                keywordSortClause,
                keywordSolrQueryConf,
                facetConfig);
        this.keywordQueryProcessorConfig = keywordQueryProcessorConfig;
        this.fieldConfig = keywordSearchFieldConfig;
        this.rdfStreamer = supportDataRdfStreamer;
        this.documentIdStream = documentIdStream;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.fieldConfig.getSearchFieldItemByName(KEYWORD_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return keywordQueryProcessorConfig;
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<KeywordDocument> getDocumentIdStream() {
        return this.documentIdStream;
    }

    @Override
    protected SolrRequest.SolrRequestBuilder createSolrRequestBuilder(
            BasicRequest request,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig queryBoosts) {
        KeywordBasicRequest kwRequest = (KeywordBasicRequest) request;
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
        if (KEYWORD_ID_REGEX_PATTERN.matcher(cleanQuery.toUpperCase()).matches()) {
            kwRequest.setQuery(cleanQuery.toUpperCase());
        }
        return super.createSolrRequestBuilder(kwRequest, solrSortClause, queryBoosts);
    }
}
