package org.uniprot.api.subcell.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.subcell.SubcellularLocationRepository;
import org.uniprot.api.subcell.request.SubcellularLocationRequestDTO;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;
import org.uniprot.store.search.field.SubcellularLocationField;

import java.util.stream.Stream;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Service
public class SubcellularLocationService {
    private final BasicSearchService<SubcellularLocationEntry, SubcellularLocationDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;
    private final SubcellularLocationSortClause subcellularLocationSortClause;

    public SubcellularLocationService(SubcellularLocationRepository repository) {
        this.basicService = new BasicSearchService<>(repository, new SubcellularLocationEntryConverter());
        this.defaultSearchHandler = new DefaultSearchHandler(SubcellularLocationField.Search.content, SubcellularLocationField.Search.id, SubcellularLocationField.Search.getBoostFields());
        this.subcellularLocationSortClause = new SubcellularLocationSortClause();
    }

    public SubcellularLocationEntry findById(final String subcellularLocationId) {
        return basicService.getEntity(SubcellularLocationField.Search.id.name(), subcellularLocationId);
    }

    public QueryResult<SubcellularLocationEntry> search(SubcellularLocationRequestDTO request) {
        SolrRequest solrRequest = basicService.createSolrRequest(request, null, subcellularLocationSortClause, defaultSearchHandler);
        return basicService.search(solrRequest, request.getCursor(), request.getSize());
    }

    public Stream<SubcellularLocationEntry> download(SubcellularLocationRequestDTO request) {
        SolrRequest solrRequest = basicService.createSolrRequest(request, null, subcellularLocationSortClause, defaultSearchHandler);
        return basicService.download(solrRequest);
    }
}