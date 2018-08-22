package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QueryCursorRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.QuerySearchRequest;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.query.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRespository;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.CloudSolrStreamTemplate;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.StoreStreamer;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service class responsible to build Solr query and execute it in the repository.
 *
 * @author lgonzales
 */

@Service
public class UniprotAdvancedSearchService {
    private final CloudSolrStreamTemplate cloudSolrStreamTemplate;
    private final UniprotQueryRespository repository;
    private final UniprotFacetConfig uniprotFacetConfig;
    private final StoreStreamer<UniProtEntry> storeStreamer;

    public UniprotAdvancedSearchService(UniprotQueryRespository repository,
                                        UniprotFacetConfig uniprotFacetConfig,
                                        CloudSolrStreamTemplate cloudSolrStreamTemplate,
                                        StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer) {
        this.repository = repository;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.cloudSolrStreamTemplate = cloudSolrStreamTemplate;
        this.storeStreamer = uniProtEntryStoreStreamer;
    }

    public QueryResult<UniProtDocument> executeQuery(QuerySearchRequest searchRequest) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(searchRequest.getQuery(), uniprotFacetConfig).build();
            return repository.searchPage(simpleQuery, searchRequest.getOffset(), searchRequest.getSize());
        } catch (Exception e) {
            String message = "Could not get result for: [" + searchRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    public QueryResult<UniProtDocument> executeCursorQuery(QueryCursorRequest cursorRequest) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(cursorRequest.getQuery(), uniprotFacetConfig).build();
            simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
            return repository.searchCursorPage(simpleQuery, cursorRequest.getCursor(), cursorRequest.getSize());
        } catch (Exception e) {
            String message = "Could not get result for: [" + cursorRequest + "]";
            throw new ServiceException(message, e);
        }
    }

    public Cursor<UniProtDocument> getAll(String query) {
        try {
            SimpleQuery simpleQuery = SolrQueryBuilder.of(query, uniprotFacetConfig).build();
            simpleQuery.addSort(new Sort(Sort.Direction.ASC, "accession"));
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

    public Stream<Collection<UniProtEntry>> stream(String query) {
        CloudSolrStream cStream = cloudSolrStreamTemplate.create(query);
        try {
            cStream.open();
            return storeStreamer.searchToStoreStream(cStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
