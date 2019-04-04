package uk.ac.ebi.uniprot.api.configure.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.query.SolrJsonQuery;
import uk.ac.ebi.uniprot.api.configure.util.SolrQueryConverter;
import uk.ac.ebi.uniprot.common.Utils;

@Service
public class UtilService {

    public SolrJsonQuery convertQuery(String query){
        SolrJsonQuery solrJsonQuery = null;
        if(Utils.notEmpty(query)) {
            try {
                QueryParser qp = new QueryParser("", new StandardAnalyzer());
                qp.setAllowLeadingWildcard(true);
                Query queryObject = qp.parse(query);
                solrJsonQuery = SolrQueryConverter.convert(queryObject);
            } catch (Exception e) {
                throw new RuntimeException("Invalid query requested: "+query,e);
            }
        }else {
            throw new RuntimeException("Query is required");
        }
        return solrJsonQuery;
    }

}
