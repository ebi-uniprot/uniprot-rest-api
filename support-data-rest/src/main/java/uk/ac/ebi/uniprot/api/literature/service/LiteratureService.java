package uk.ac.ebi.uniprot.api.literature.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.literature.LiteratureRepository;
import uk.ac.ebi.uniprot.api.literature.request.LiteratureRequestDTO;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.literature.LiteratureDocument;
import uk.ac.ebi.uniprot.search.field.LiteratureField;

import java.util.stream.Stream;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Service
public class LiteratureService {
    private final BasicSearchService<LiteratureEntry, LiteratureDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;
    private final LiteratureSortClause literatureSortClause;

    public LiteratureService(LiteratureRepository repository) {
        this.basicService = new BasicSearchService<>(repository, new LiteratureEntryConverter());
        this.defaultSearchHandler = new DefaultSearchHandler(LiteratureField.Search.content, LiteratureField.Search.id, LiteratureField.Search.getBoostFields());
        this.literatureSortClause = new LiteratureSortClause();
    }

    public LiteratureEntry findById(final String literatureId) {
        return basicService.getEntity(LiteratureField.Search.id.name(), literatureId);
    }

    public QueryResult<LiteratureEntry> search(LiteratureRequestDTO request) {
        SolrRequest solrRequest = basicService.createSolrRequest(request, null, literatureSortClause, defaultSearchHandler);
        return basicService.search(solrRequest, request.getCursor(), request.getSize());
    }

    public Stream<LiteratureEntry> download(LiteratureRequestDTO request) {
        SolrRequest solrRequest = basicService.createSolrRequest(request, null, literatureSortClause, defaultSearchHandler);
        return basicService.download(solrRequest);
    }
}
