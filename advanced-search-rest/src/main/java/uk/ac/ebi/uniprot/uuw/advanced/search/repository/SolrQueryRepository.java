package uk.ac.ebi.uniprot.uuw.advanced.search.repository;


import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.QueryResult;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.facet.Facet;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.impl.CursorPage;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.facet.FacetConfigConverter;

import java.util.List;
import java.util.Optional;

/**
 * Solr Basic Repository class to enable the execution of dynamically build queries in a solr collections.
 * <p>
 * It was defined a common QueryResult object in order to be able to
 *
 * @param <T> Returned Solr entity
 *
 * @author lgonzales
 */
public abstract class SolrQueryRepository<T> {

    private static final Integer DEFAULT_PAGE_SIZE = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryRepository.class);

    private SolrTemplate solrTemplate;
    private SolrCollection collection;
    private Class<T> tClass;
    private FacetConfigConverter facetConverter;

    protected SolrQueryRepository(SolrTemplate solrTemplate, SolrCollection collection, Class<T> tClass, FacetConfigConverter facetConverter) {
        this.solrTemplate = solrTemplate;
        this.collection = collection;
        this.tClass = tClass;
        this.facetConverter = facetConverter;
    }

    public QueryResult<T> searchPage(SimpleQuery query, String cursor,Integer pageSize) {
        if(pageSize == null || pageSize <=0){
            pageSize = DEFAULT_PAGE_SIZE;
        }
        try {
            CursorPage page = CursorPage.of(cursor, pageSize);
            QueryResponse solrResponse = solrTemplate.execute(getSolrCursorCallback(query, page.getCursor(), pageSize));

            List<T> resultList = solrTemplate.convertQueryResponseToBeans(solrResponse, tClass);
            page.setNextCursor(solrResponse.getNextCursorMark());
            page.setTotalElements(solrResponse.getResults().getNumFound());

            List<Facet> facets = facetConverter.convert(solrResponse);

            return QueryResult.of(resultList, page, facets);
        } catch (Throwable e) {
            throw new QueryRetrievalException("Unexpected error retrieving data from our Repository", e);
        } finally {
            logSolrQuery(query);
        }
    }

    public Optional<T> getEntry(SimpleQuery query) {
        try {
            return solrTemplate.queryForObject(collection.toString(), query, tClass);
        } catch (Throwable e) {
            throw new QueryRetrievalException("Error executing solr query", e);
        } finally {
            logSolrQuery(query);
        }
    }


    private SolrCallback<QueryResponse> getSolrCursorCallback(SimpleQuery query, String cursor,Integer pageSize) {
        return solrClient -> {
            DefaultQueryParser queryParser = new DefaultQueryParser();
            SolrQuery solrQuery = queryParser.doConstructSolrQuery(query);

            if(cursor != null && !cursor.isEmpty()) {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
            }else {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
            }
            solrQuery.setRows(pageSize);

            return solrClient.query(collection.toString(), solrQuery);
        };
    }

    private void logSolrQuery(SimpleQuery query){
        if(query != null) {
            DefaultQueryParser queryParser = new DefaultQueryParser();
            LOGGER.debug("solrQuery: " + queryParser.getQueryString(query));
        }
    }

}
