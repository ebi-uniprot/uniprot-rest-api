package org.uniprot.api.support.data.keyword.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.support.data.keyword.repository.KeywordFacetConfig;
import org.uniprot.api.support.data.keyword.repository.KeywordRepository;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.keyword.KeywordDocument;

@Service
@Import(KeywordSolrQueryConfig.class)
public class KeywordService extends BasicSearchService<KeywordDocument, KeywordEntry> {
    public static final String KEYWORD_ID_FIELD = "keyword_id";
    private final SearchFieldConfig fieldConfig;
    private final QueryProcessor queryProcessor;
    private final RDFStreamer rdfStreamer;

    public KeywordService(
            KeywordRepository repository,
            KeywordEntryConverter keywordEntryConverter,
            KeywordSortClause keywordSortClause,
            SolrQueryConfig keywordSolrQueryConf,
            QueryProcessor keywordQueryProcessor,
            KeywordFacetConfig facetConfig,
            SearchFieldConfig keywordSearchFieldConfig,
            @Qualifier("keywordRDFStreamer") RDFStreamer rdfStreamer) {
        super(
                repository,
                keywordEntryConverter,
                keywordSortClause,
                keywordSolrQueryConf,
                facetConfig);
        this.fieldConfig = keywordSearchFieldConfig;
        this.queryProcessor = keywordQueryProcessor;
        this.rdfStreamer = rdfStreamer;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.fieldConfig.getSearchFieldItemByName(KEYWORD_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return this.rdfStreamer;
    }
}
