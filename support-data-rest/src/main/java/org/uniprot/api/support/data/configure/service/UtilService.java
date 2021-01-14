package org.uniprot.api.support.data.configure.service;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.SolrJsonQuery;
import org.uniprot.core.util.Utils;

@Service
public class UtilService {

    public SolrJsonQuery convertQuery(String query) {
        SolrJsonQuery solrJsonQuery = null;
        if (Utils.notNullNotEmpty(query)) {
            try {
                QueryParser qp = new QueryParser("", new WhitespaceAnalyzer());
                qp.setAllowLeadingWildcard(true);
                Query queryObject = qp.parse(query);
                solrJsonQuery = SolrQueryConverter.convert(queryObject);
            } catch (Exception e) {
                throw new RuntimeException("Invalid query requested: " + query, e);
            }
        } else {
            throw new RuntimeException("Query is required");
        }
        return solrJsonQuery;
    }
}
