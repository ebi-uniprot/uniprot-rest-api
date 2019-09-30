package org.uniprot.api.disease;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.core.cv.disease.Disease;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.disease.DiseaseDocument;
import org.uniprot.store.search.field.DiseaseField;

@Service
public class DiseaseService {
    private BasicSearchService<Disease, DiseaseDocument> basicService;
    private DefaultSearchHandler defaultSearchHandler;

    @Autowired private DiseaseSolrSortClause solrSortClause;

    @Autowired
    public void setDefaultSearchHandler() {
        this.defaultSearchHandler =
                new DefaultSearchHandler(
                        DiseaseField.Search.content,
                        DiseaseField.Search.accession,
                        DiseaseField.Search.getBoostFields());
    }

    @Autowired
    public void setBasicService(
            DiseaseRepository diseaseRepository,
            DiseaseDocumentToDiseaseConverter toDiseaseConverter) {
        this.basicService = new BasicSearchService<>(diseaseRepository, toDiseaseConverter);
    }

    public Disease findByAccession(final String accession) {

        return this.basicService.getEntity(DiseaseField.Search.accession.name(), accession);
    }

    public QueryResult<Disease> search(DiseaseSearchRequest request) {

        SolrRequest solrRequest =
                this.basicService.createSolrRequest(
                        request, null, this.solrSortClause, this.defaultSearchHandler);

        return this.basicService.search(solrRequest, request.getCursor(), request.getSize());
    }

    public Stream<Disease> download(DiseaseSearchRequest request) {

        SolrRequest query =
                this.basicService.createSolrRequest(
                        request, null, this.solrSortClause, this.defaultSearchHandler);

        return this.basicService.download(query);
    }
}
