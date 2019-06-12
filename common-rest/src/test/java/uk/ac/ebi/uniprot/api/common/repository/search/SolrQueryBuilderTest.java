package uk.ac.ebi.uniprot.api.common.repository.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleTermsQuery;

/**
 * Created 12/06/19
 *
 * @author Edd
 */
class SolrQueryBuilderTest {
    private SolrQueryBuilder builder;

    @BeforeEach
    void setUp() {
        this.builder = new SolrQueryBuilder();
    }

    @Test
    void settingQueryIsCorrect() {
        String query = "my query";
        builder.query(query);
        SimpleQuery simpleQuery = builder.build();
        SimpleTermsQuery.Builder builder = new SimpleTermsQuery.Builder().withCriteria(simpleQuery.getCriteria());
        builder.fields("field1", "field2");
        SimpleTermsQuery termsQuery = builder.build();

        DefaultQueryParser queryParser = new DefaultQueryParser();
        SolrQuery solrQuery = queryParser.doConstructSolrQuery(termsQuery);
        solrQuery.setParam("terms", "true");


        System.out.println(solrQuery);
    }
}