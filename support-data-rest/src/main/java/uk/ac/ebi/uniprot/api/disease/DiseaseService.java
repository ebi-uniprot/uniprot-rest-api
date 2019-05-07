package uk.ac.ebi.uniprot.api.disease;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.cv.disease.Disease;
import uk.ac.ebi.uniprot.json.parser.disease.DiseaseJsonConfig;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiseaseService {
    private static final String ACCESSION_STR = "accession";
    @Autowired
    private DiseaseRepository diseaseRepository;
    @Autowired
    private DiseaseSolrSortClause solrSortClause;
    @Autowired
    private DiseaseDocumentToDiseaseConverter toDiseaseConverter;

    private ObjectMapper diseaseObjectMapper = DiseaseJsonConfig.getInstance().getFullObjectMapper();

    public Disease findByAccession(final String accession) {
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION_STR).is(accession.toUpperCase()));
            Optional<DiseaseDocument> optionalDoc = this.diseaseRepository.getEntry(simpleQuery);

            if (optionalDoc.isPresent()) {
                DiseaseDocument disDoc = optionalDoc.get();
                ByteBuffer diseaseBB = disDoc.getDiseaseObj();
                Disease disease = this.diseaseObjectMapper.readValue(diseaseBB.array(), Disease.class);
                return disease;
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
        SimpleQuery simpleQuery = createQuery(request);
        QueryResult<DiseaseDocument> results = this.diseaseRepository.searchPage(simpleQuery, request.getCursor(), request.getSize());
        List<Disease> converted = results.getContent().stream()
                .map(toDiseaseConverter)
                .filter(val -> val != null)
                .collect(Collectors.toList());

        return QueryResult.of(converted, results.getPage());
    }

    private SimpleQuery createQuery(DiseaseSearchRequest request) {
        SolrQueryBuilder builder = new SolrQueryBuilder();
        String requestedQuery = request.getQuery();

        builder.query(requestedQuery);
        builder.addSort(this.solrSortClause.getSort(request.getSort(), false));

        return builder.build();
    }
}
