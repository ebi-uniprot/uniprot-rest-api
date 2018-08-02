package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.UniprotQueryRespository;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * Service class responsible to build Solr query and execute it in the repository.
 *
 * @author lgonzales
 */

@Service
public class UniprotAdvancedSearchService {

    @Resource
    private UniprotQueryRespository repository;

    public QueryResult<UniProtDocument> executeQuery(QuerySearchRequest searchRequest) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(searchRequest.getQuery()).build();
            return repository.searchPage(simpleQuery,searchRequest.getOffset(),searchRequest.getSize());
        } catch (Exception e) {
            String message = "Could not get result for: [" + searchRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    public QueryResult<UniProtDocument> executeCursorQuery(QueryCursorRequest cursorRequest) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery()).build();
            simpleQuery.addSort(new Sort(Sort.Direction.ASC,"accession"));
            return repository.searchCursorPage(simpleQuery,cursorRequest.getCursor(),cursorRequest.getSize());
        } catch (Exception e) {
            String message = "Could not get result for: [" + cursorRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    public Cursor<UniProtDocument> getAll(String query) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(query).build();
            simpleQuery.addSort(new Sort(Sort.Direction.ASC,"accession"));
            return repository.getAll(simpleQuery);
        } catch (Exception e) {
            String message = "Could not get result for: [" + query + "]";
            throw new ServiceException(message, e);
        }
    }


    public Optional<UniProtDocument> getByAccession(String accession) {
        try {
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where("accession").is(accession.toUpperCase()));
            return repository.getEntry(simpleQuery);
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

}
