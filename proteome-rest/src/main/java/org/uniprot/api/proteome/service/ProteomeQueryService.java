package org.uniprot.api.proteome.service;

import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.proteome.request.ProteomeRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.proteome.ProteomeDocument;
import org.uniprot.store.search.field.ProteomeField.Search;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
public class ProteomeQueryService {
    private ProteomeFacetConfig facetConfig;
    private final ProteomeSortClause solrSortClause;
    private final DefaultSearchHandler defaultSearchHandler;
    private final BasicSearchService<ProteomeEntry, ProteomeDocument> basicService;

    public ProteomeQueryService(
            ProteomeQueryRepository repository,
            ProteomeFacetConfig facetConfig,
            ProteomeSortClause solrSortClause) {
        this.facetConfig = facetConfig;
        this.solrSortClause = solrSortClause;
        this.defaultSearchHandler =
                new DefaultSearchHandler(Search.content, Search.upid, Search.getBoostFields());
        basicService = new BasicSearchService<>(repository, new ProteomeEntryConverter());
    }

    public QueryResult<ProteomeEntry> search(ProteomeRequest request) {
        SolrRequest query =
                basicService.createSolrRequest(
                        request, facetConfig, solrSortClause, defaultSearchHandler);
        return basicService.search(query, request.getCursor(), request.getSize());
    }

    public ProteomeEntry getByUPId(String upid) {
        return basicService.getEntity(Search.upid.name(), upid.toUpperCase());
    }

    public Stream<ProteomeEntry> download(ProteomeRequest request) {
        SolrRequest query =
                basicService.createSolrRequest(
                        request, facetConfig, solrSortClause, defaultSearchHandler);
        return basicService.download(query);
    }
}
