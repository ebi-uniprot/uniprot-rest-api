package uk.ac.ebi.uniprot.api.disease;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.rest.service.BasicSearchService;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;
import uk.ac.ebi.uniprot.search.field.DiseaseField;

@Service
public class DiseaseService {
    private BasicSearchService<Disease, DiseaseDocument> basicService;
    private DefaultSearchHandler defaultSearchHandler;

    @Autowired
    private DiseaseSolrSortClause solrSortClause;

    @Autowired
    public void setDefaultSearchHandler() {
        this.defaultSearchHandler = new DefaultSearchHandler(DiseaseField.Search.content,
                DiseaseField.Search.accession, DiseaseField.Search.getBoostFields());
    }

    @Autowired
    public void setBasicService(DiseaseRepository diseaseRepository, DiseaseDocumentToDiseaseConverter toDiseaseConverter) {
        this.basicService = new BasicSearchService<>(diseaseRepository, toDiseaseConverter);
    }

    public Disease findByAccession(final String accession) {

        return this.basicService.getEntity(DiseaseField.Search.accession.name(), accession);
    }

    public QueryResult<Disease> search(DiseaseSearchRequest request) {

        SolrRequest solrRequest = this.basicService.createSolrRequest(request, null, this.solrSortClause, this.defaultSearchHandler);

        return this.basicService.search(solrRequest, request.getCursor(), request.getSize());
    }
}
