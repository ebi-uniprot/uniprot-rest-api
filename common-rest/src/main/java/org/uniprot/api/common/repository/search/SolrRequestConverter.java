package org.uniprot.api.common.repository.search;

import static org.uniprot.api.common.repository.search.SolrQueryConverterUtils.*;
import static org.uniprot.core.util.Utils.nullOrEmpty;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.uniprot.api.common.exception.InvalidRequestException;

import lombok.extern.slf4j.Slf4j;

/**
 * Created 14/06/19
 *
 * @author Edd
 */
@Slf4j
public class SolrRequestConverter {
    /**
     * Creates a {@link SolrQuery} from a {@link SolrRequest}.
     *
     * @param request the request that specifies the query
     * @return the solr query
     */
    public JsonQueryRequest toJsonQueryRequest(SolrRequest request) {
        return toJsonQueryRequest(request, false);
    }

    public JsonQueryRequest toJsonQueryRequest(SolrRequest request, boolean isEntry) {
        final ModifiableSolrParams solrQuery = new ModifiableSolrParams();
        solrQuery.add("q", request.getQuery());

        if (!isEntry) {
            setDefaults(solrQuery, request.getDefaultField());
        }

        solrQuery.add("rows", String.valueOf(request.getRows()));
        setFilterQueries(solrQuery, request.getFilterQueries());
        setQueryOperator(solrQuery, request.getDefaultQueryOperator());
        if (!request.getTermFields().isEmpty()) {
            if (nullOrEmpty(request.getTermQuery())) {
                throw new InvalidRequestException("Please specify required field, term query.");
            }
            setTermFields(solrQuery, request.getTermQuery(), request.getTermFields());
        }

        if (request.getRows() > 1) {
            setQueryBoostConfigs(solrQuery, request);
            setHighlightFieldsConfigs(solrQuery, request);
        }
        setQueryFields(solrQuery, request);

        JsonQueryRequest result = new JsonQueryRequest(solrQuery);
        setSort(result, request.getSorts());
        if (!request.getFacets().isEmpty()) {
            setFacets(result, request);
        }

        log.debug("Solr Query without facet and sort details: " + solrQuery);

        return result;
    }
}
