package uk.ac.ebi.uniprot.api.crossref.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.crossref.config.CrossRefFacetConfig;
import uk.ac.ebi.uniprot.api.crossref.repository.CrossRefRepository;
import uk.ac.ebi.uniprot.api.crossref.request.CrossRefSearchRequest;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;

import java.util.Optional;

@Service
public class CrossRefService {
    private static final String ACCESSION_STR = "accession";
    @Autowired
    private CrossRefRepository crossRefRepository;
    @Autowired
    private CrossRefFacetConfig crossRefFacetConfig;
    @Autowired
    private CrossRefSolrSortClause solrSortClause;

    public CrossRefDocument findByAccession(final String accession) {
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION_STR).is(accession.toUpperCase()));
            Optional<CrossRefDocument> optionalDoc = crossRefRepository.getEntry(simpleQuery);

            if (optionalDoc.isPresent()) {
                return optionalDoc.get();
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

    public QueryResult<CrossRefDocument> search(CrossRefSearchRequest request) {
        SimpleQuery simpleQuery = createQuery(request);

        QueryResult<CrossRefDocument> results = crossRefRepository.searchPage(simpleQuery, request.getCursor(), request.getSize());

        return results;
    }

    private SimpleQuery createQuery(CrossRefSearchRequest request) {
        SolrQueryBuilder builder = new SolrQueryBuilder();
        String requestedQuery = request.getQuery();

        builder.query(requestedQuery);
        builder.addSort(this.solrSortClause.getSort(request.getSort(), false));

        if(request.hasFacets()) {
            builder.facets(request.getFacetList());
            builder.facetConfig(this.crossRefFacetConfig);
        }
        return builder.build();
    }


}
