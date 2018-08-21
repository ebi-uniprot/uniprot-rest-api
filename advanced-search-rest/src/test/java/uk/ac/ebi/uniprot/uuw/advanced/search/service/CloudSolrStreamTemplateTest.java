package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
public class CloudSolrStreamTemplateTest {
    @Test
    public void canCreateBuilderWithDefaults() {
        CloudSolrStreamTemplate template = CloudSolrStreamTemplate.builder()
                .collection("defaultCollection")
                .key("defaultKey")
                .zookeeperHost("defaultZookeeperHost")
                .order(SolrQuery.ORDER.asc)
                .build();
    }

}