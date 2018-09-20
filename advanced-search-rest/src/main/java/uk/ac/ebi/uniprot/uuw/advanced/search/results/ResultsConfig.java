package uk.ac.ebi.uniprot.uuw.advanced.search.results;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.voldemort.VoldemortClient;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.RepositoryConfigProperties;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class ResultsConfig {
    @Bean
    public CloudSolrStreamTemplate cloudSolrStreamTemplate(RepositoryConfigProperties configProperties) {
        return CloudSolrStreamTemplate.builder()
                .collection("uniprot")
                .key("accession")
                .order(SolrQuery.ORDER.asc)
                .requestHandler("/export")
                .zookeeperHost(configProperties.getZookeperhost())
                .build();
    }

    @Bean
    public StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer(VoldemortClient uniProtClient) {
        return new StoreStreamer<>(uniProtClient,
                                   resultsConfigProperties().getUniprot().getBatchSize(),
                                   resultsConfigProperties().getUniprot().getValueId());
    }

    @Bean
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }

}
