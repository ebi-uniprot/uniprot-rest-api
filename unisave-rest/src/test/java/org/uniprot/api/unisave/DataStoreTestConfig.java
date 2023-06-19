package org.uniprot.api.unisave;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile({"offline", "unisave-controller-test"})
public class DataStoreTestConfig {

    @Bean
    public SolrClient solrClient() {
        return mock(SolrClient.class);
    }

    @Bean
    public SolrRequestConverter solrRequestConverter() {
        return mock(SolrRequestConverter.class);
    }

    @Bean
    public RdfStreamer rdfStreamer() {
        return mock(RdfStreamer.class);
    }
}
