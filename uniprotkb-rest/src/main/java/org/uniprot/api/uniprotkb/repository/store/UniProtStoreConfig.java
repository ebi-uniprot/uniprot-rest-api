package org.uniprot.api.uniprotkb.repository.store;

import java.util.Collections;

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
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteUniProtKBEntryStore;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class UniProtStoreConfig {
    @Bean
    public UniProtStoreConfigProperties storeConfigProperties() {
        return new UniProtStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public UniProtKBStoreClient uniProtStoreClient(
            UniProtStoreConfigProperties uniProtStoreConfigProperties) {
        VoldemortClient<UniProtEntry> client =
                new VoldemortRemoteUniProtKBEntryStore(
                        uniProtStoreConfigProperties.getNumberOfConnections(),
                        uniProtStoreConfigProperties.getStoreName(),
                        uniProtStoreConfigProperties.getHost());
        return new UniProtKBStoreClient(client);
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
