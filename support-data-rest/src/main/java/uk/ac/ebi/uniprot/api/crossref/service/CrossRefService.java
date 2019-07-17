package uk.ac.ebi.uniprot.api.crossref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.crossref.config.CrossRefFacetConfig;
import uk.ac.ebi.uniprot.api.crossref.repository.CrossRefRepository;
import uk.ac.ebi.uniprot.api.crossref.request.CrossRefEntryConverter;
import uk.ac.ebi.uniprot.api.crossref.request.CrossRefSearchRequest;
import uk.ac.ebi.uniprot.api.disease.DiseaseSolrSortClause;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;
import uk.ac.ebi.uniprot.search.field.CrossRefField;

@Service
public class CrossRefService {
    private BasicSearchService<CrossRefEntry, CrossRefDocument> basicService;
    private DefaultSearchHandler defaultSearchHandler;

    @Autowired
    private DiseaseSolrSortClause solrSortClause;
    @Autowired
    private CrossRefFacetConfig crossRefFacetConfig;

    @Autowired
    public void setDefaultSearchHandler() {
        this.defaultSearchHandler = new DefaultSearchHandler(CrossRefField.Search.content,
                CrossRefField.Search.accession, CrossRefField.Search.getBoostFields());
    }

    @Autowired
    public void setBasicService(CrossRefRepository crossRefRepository, CrossRefEntryConverter toCrossRefEntryConverter) {
        this.basicService = new BasicSearchService<>(crossRefRepository, toCrossRefEntryConverter);
    }


    public CrossRefEntry findByAccession(final String accession) {
        return this.basicService.getEntity(CrossRefField.Search.accession.name(), accession);
    }

    public QueryResult<CrossRefEntry> search(CrossRefSearchRequest request) {

        SolrRequest solrRequest = this.basicService.createSolrRequest(request, this.crossRefFacetConfig,
                this.solrSortClause, this.defaultSearchHandler);

        return this.basicService.search(solrRequest, request.getCursor(), request.getSize());
    }
}