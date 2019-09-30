package org.uniprot.api.uniparc.service;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.request.UniParcRequest;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.field.UniParcField.Search;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
public class UniParcQueryService {
    private final UniParcSortClause solrSortClause;
    private UniParcFacetConfig facetConfig;
    private final BasicSearchService<UniParcEntry, UniParcDocument> basicService;
    private final DefaultSearchHandler defaultSearchHandler;

    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcSortClause solrSortClause) {
        basicService = new BasicSearchService<>(repository, new UniParcEntryConverter());
        this.facetConfig = facetConfig;
        this.solrSortClause = solrSortClause;
        this.defaultSearchHandler =
                new DefaultSearchHandler(Search.content, Search.upi, Search.getBoostFields());
    }

    public QueryResult<UniParcEntry> search(UniParcRequest request) {
        SolrRequest query =
                basicService.createSolrRequest(
                        request, facetConfig, solrSortClause, defaultSearchHandler);
        return basicService.search(query, request.getCursor(), request.getSize());
    }

    public UniParcEntry getById(String upi) {
        return basicService.getEntity(Search.upi.name(), upi.toUpperCase());
    }

    public Stream<UniParcEntry> download(UniParcRequest request) {
        SolrRequest query =
                basicService.createSolrRequest(
                        request, facetConfig, solrSortClause, defaultSearchHandler);
        return basicService.download(query);
    }
}
