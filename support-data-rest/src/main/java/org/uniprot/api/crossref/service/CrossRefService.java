package org.uniprot.api.crossref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.crossref.config.CrossRefFacetConfig;
import org.uniprot.api.crossref.repository.CrossRefRepository;
import org.uniprot.api.crossref.request.CrossRefEntryConverter;
import org.uniprot.api.crossref.request.CrossRefSearchRequest;
import org.uniprot.api.disease.DiseaseSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.crossref.CrossRefEntry;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;
import org.uniprot.store.search.field.CrossRefField;

@Service
public class CrossRefService {
    private BasicSearchService<CrossRefEntry, CrossRefDocument> basicService;
    private DefaultSearchHandler defaultSearchHandler;

    @Autowired private DiseaseSolrSortClause solrSortClause;
    @Autowired private CrossRefFacetConfig crossRefFacetConfig;

    @Autowired
    public void setDefaultSearchHandler() {
        this.defaultSearchHandler =
                new DefaultSearchHandler(
                        CrossRefField.Search.content,
                        CrossRefField.Search.accession,
                        CrossRefField.Search.getBoostFields());
    }

    @Autowired
    public void setBasicService(
            CrossRefRepository crossRefRepository,
            CrossRefEntryConverter toCrossRefEntryConverter) {
        this.basicService = new BasicSearchService<>(crossRefRepository, toCrossRefEntryConverter);
    }

    public CrossRefEntry findByAccession(final String accession) {
        return this.basicService.getEntity(CrossRefField.Search.accession.name(), accession);
    }

    public QueryResult<CrossRefEntry> search(CrossRefSearchRequest request) {

        SolrRequest solrRequest =
                this.basicService.createSolrRequest(
                        request,
                        this.crossRefFacetConfig,
                        this.solrSortClause,
                        this.defaultSearchHandler);

        return this.basicService.search(solrRequest, request.getCursor(), request.getSize());
    }
}
