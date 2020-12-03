package org.uniprot.api.uniparc.repository.store;

import java.util.Collections;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.store.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniparc.VoldemortRemoteUniParcEntryStore;

/**
 * @author lgonzales
 * @since 2020-03-04
 */
@Configuration
@EnableConfigurationProperties({UniParcStoreConfigProperties.class})
public class UniParcStoreConfig {

    @Bean
    @Profile("live")
    public UniParcStoreClient uniParcStoreClient(
            UniParcStoreConfigProperties uniParcStoreConfigProperties) {
        VoldemortClient<UniParcEntry> client =
                new VoldemortRemoteUniParcEntryStore(
                        uniParcStoreConfigProperties.getNumberOfConnections(),
                        uniParcStoreConfigProperties.getStoreName(),
                        uniParcStoreConfigProperties.getHost());
        return new UniParcStoreClient(client);
    }

    @Bean(name = "rdfRestTemplate")
    @Profile("live")
    RestTemplate restTemplate(RDFStreamerConfigProperties rdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(rdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
