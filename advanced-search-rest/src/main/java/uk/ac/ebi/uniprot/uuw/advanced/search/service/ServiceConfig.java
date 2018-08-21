package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.RepositoryConfigProperties;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class ServiceConfig {
    @Bean
    public CloudSolrStreamTemplate cloudSolrStreamTemplate(RepositoryConfigProperties configProperties) {
        return CloudSolrStreamTemplate.builder()
                .collection("uniprot")
                .key("accession_exact")
                .order(SolrQuery.ORDER.asc)
                .requestHandler("/export")
                .zookeeperHost(configProperties.getZookeperhost())
                .build();
    }
}
