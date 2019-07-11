package uk.ac.ebi.uniprot.api.disease;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiseaseService {
    private static final String ACCESSION_STR = "accession:";
    @Autowired
    private DiseaseRepository diseaseRepository;
    @Autowired
    private DiseaseSolrSortClause solrSortClause;
    @Autowired
    private DiseaseDocumentToDiseaseConverter toDiseaseConverter;

    private ObjectMapper diseaseObjectMapper = DiseaseJsonConfig.getInstance().getFullObjectMapper();

    public Disease findByAccession(final String accession) {
        try {
            Optional<DiseaseDocument> optionalDoc = this.diseaseRepository.getEntry(SolrRequest
                                                                                            .builder().query(ACCESSION_STR + accession.toUpperCase()).build());

            if (optionalDoc.isPresent()) {
                DiseaseDocument disDoc = optionalDoc.get();
                ByteBuffer diseaseBB = disDoc.getDiseaseObj();
                return this.diseaseObjectMapper.readValue(diseaseBB.array(), Disease.class);
            } else {
                throw new ResourceNotFoundException("{search.not.found}");
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public QueryResult<Disease> search(DiseaseSearchRequest request) {
        SolrRequest solrRequest = createQuery(request);
        QueryResult<DiseaseDocument> results = this.diseaseRepository.searchPage(solrRequest, request.getCursor(), request.getSize());
        List<Disease> converted = results.getContent().stream()
                .map(toDiseaseConverter)
                .filter(Utils::nonNull)
                .collect(Collectors.toList());

        return QueryResult.of(converted, results.getPage());
    }

    private SolrRequest createQuery(DiseaseSearchRequest request) {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String requestedQuery = request.getQuery();

        builder.query(requestedQuery);
        builder.addSort(this.solrSortClause.getSort(request.getSort(), false));

        return builder.build();
    }
}
