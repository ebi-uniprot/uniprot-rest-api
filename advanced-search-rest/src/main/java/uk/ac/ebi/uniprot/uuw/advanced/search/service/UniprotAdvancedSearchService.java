package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
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
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRespository;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Service class responsible to build Solr query and execute it in the repository.
 *
 * @author lgonzales
 */

@Service
public class UniprotAdvancedSearchService {

    private UniprotQueryRespository repository;
    private UniprotFacetConfig uniprotFacetConfig;

    public UniprotAdvancedSearchService(UniprotQueryRespository repository, UniprotFacetConfig uniprotFacetConfig){
        this.repository = repository;
        this.uniprotFacetConfig = uniprotFacetConfig;
    }

    public QueryResult<UniProtDocument> executeQuery(QuerySearchRequest searchRequest) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(searchRequest.getQuery(),uniprotFacetConfig).build();
            return repository.searchPage(simpleQuery,searchRequest.getOffset(),searchRequest.getSize());
        } catch (Exception e) {
            String message = "Could not get result for: [" + searchRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    public QueryResult<UniProtDocument> executeCursorQuery(QueryCursorRequest cursorRequest) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery(),uniprotFacetConfig).build();
            simpleQuery.addSort(new Sort(Sort.Direction.ASC,"accession"));
            return repository.searchCursorPage(simpleQuery,cursorRequest.getCursor(),cursorRequest.getSize());
        } catch (Exception e) {
            String message = "Could not get result for: [" + cursorRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    public Cursor<UniProtDocument> getAll(String query) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(query,uniprotFacetConfig).build();
            simpleQuery.addSort(new Sort(Sort.Direction.ASC,"accession"));
            return repository.getAll(simpleQuery);
        } catch (Exception e) {
            String message = "Could not get result for: [" + query + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<String> streamAll(String query) {
        // TODO: 17/08/18 get zkHost from config
        String zkHost = "FROM CONFIGURATION";
        String collection = "uniprot";

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        String solrField = "accession";
        solrQuery.setSort(solrField, SolrQuery.ORDER.desc);
        solrQuery.setFields(solrField);
        solrQuery.setRequestHandler("/export");

        try (CloudSolrStream cStream = new CloudSolrStream(zkHost,
                                                           collection,
                                                           solrQuery)) {
            cStream.open();
            Iterable<String> resultIterable = () -> cloudStreamToIterator(cStream);
            return StreamSupport.stream(resultIterable.spliterator(), false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
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

    static Iterator<String> cloudStreamToIterator(CloudSolrStream cStream) {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                try {
                    return !cStream.read().EOF;
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public String next() {
                try {
                    return cStream
                            .read()
                            .getString("accession");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

}
