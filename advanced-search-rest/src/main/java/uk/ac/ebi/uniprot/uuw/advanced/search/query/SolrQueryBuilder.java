package uk.ac.ebi.uniprot.uuw.advanced.search.query;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;

/**
 * This class is responsible to parse request query string to {@link SimpleQuery}
 *
 * @author lgonzales
 */
public class SolrQueryBuilder {

    private final String query;

    private SolrQueryBuilder(String query){
        this.query = query;
    }

    public static SolrQueryBuilder of(String query){
        return new SolrQueryBuilder(query);
    }

    public SimpleQuery build(){
        String[] queryArray = query.split(":");
        return new SimpleQuery(Criteria.where(queryArray[0]).is(queryArray[1]));
    }

}
