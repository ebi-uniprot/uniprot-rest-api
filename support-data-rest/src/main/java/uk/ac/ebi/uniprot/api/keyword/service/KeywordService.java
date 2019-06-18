package uk.ac.ebi.uniprot.api.keyword.service;

import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.keyword.KeywordRepository;
import uk.ac.ebi.uniprot.api.keyword.request.KeywordRequestDTO;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.keyword.KeywordDocument;
import uk.ac.ebi.uniprot.search.field.KeywordField;

import java.util.stream.Stream;

@Service
public class KeywordService {
    private final BasicSearchService<KeywordEntry, KeywordDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;
    private final KeywordSortClause keywordSortClause;

    public KeywordService(KeywordRepository repository) {
        this.basicService = new BasicSearchService<>(repository, new KeywordEntryConverter());
        this.defaultSearchHandler = new DefaultSearchHandler(KeywordField.Search.content, KeywordField.Search.id, KeywordField.Search.getBoostFields());
        this.keywordSortClause = new KeywordSortClause();
    }

    public KeywordEntry findById(final String keywordId) {
        return basicService.getEntity(KeywordField.Search.keyword_id.name(), keywordId);
    }

    public QueryResult<KeywordEntry> search(KeywordRequestDTO request) {
        SimpleQuery query = basicService.createSolrQuery(request, null, keywordSortClause, defaultSearchHandler);
        return basicService.search(query, request.getCursor(), request.getSize());
    }

    public Stream<KeywordEntry> download(KeywordRequestDTO request) {
        SimpleQuery query = basicService.createSolrQuery(request, null, keywordSortClause, defaultSearchHandler);
        return basicService.download(query);
    }
}
