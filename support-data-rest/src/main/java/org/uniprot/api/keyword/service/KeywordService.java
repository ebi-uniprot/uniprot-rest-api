package org.uniprot.api.keyword.service;

import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.keyword.KeywordRepository;
import org.uniprot.api.keyword.request.KeywordRequestDTO;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.keyword.KeywordDocument;
import org.uniprot.store.search.field.KeywordField;

@Service
public class KeywordService {
    private final BasicSearchService<KeywordEntry, KeywordDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;
    private final KeywordSortClause keywordSortClause;

    public KeywordService(KeywordRepository repository) {
        this.basicService = new BasicSearchService<>(repository, new KeywordEntryConverter());
        this.defaultSearchHandler =
                new DefaultSearchHandler(
                        KeywordField.Search.content,
                        KeywordField.Search.id,
                        KeywordField.Search.getBoostFields());
        this.keywordSortClause = new KeywordSortClause();
    }

    public KeywordEntry findById(final String keywordId) {
        return basicService.getEntity(KeywordField.Search.keyword_id.name(), keywordId);
    }

    public QueryResult<KeywordEntry> search(KeywordRequestDTO request) {
        SolrRequest solrRequest =
                basicService.createSolrRequest(
                        request, null, keywordSortClause, defaultSearchHandler);
        return basicService.search(solrRequest, request.getCursor(), request.getSize());
    }

    public Stream<KeywordEntry> download(KeywordRequestDTO request) {
        SolrRequest solrRequest =
                basicService.createSolrRequest(
                        request, null, keywordSortClause, defaultSearchHandler);
        return basicService.download(solrRequest);
    }
}
