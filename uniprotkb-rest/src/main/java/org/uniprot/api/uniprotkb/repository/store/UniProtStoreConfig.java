package org.uniprot.api.uniprotkb.repository.store;

import org.springframework.cache.CacheManager;
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
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortRemoteCachingUniProtKBEntryStore;

import java.util.Collections;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class UniProtStoreConfig {
    private static final String UNIPROTKB_EH_CACHE_NAME = "uniProtKBEntryCache";

    @Bean
    public UniProtStoreConfigProperties storeConfigProperties() {
        return new UniProtStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public UniProtKBStoreClient uniProtStoreClient(
            UniProtStoreConfigProperties uniProtStoreConfigProperties, CacheManager cacheManager) {
        VoldemortRemoteCachingUniProtKBEntryStore uniProtKBEntryStore =
                new VoldemortRemoteCachingUniProtKBEntryStore(
                        uniProtStoreConfigProperties.getNumberOfConnections(),
                        uniProtStoreConfigProperties.getStoreName(),
                        uniProtStoreConfigProperties.getHost());
        uniProtKBEntryStore.setCache(cacheManager.getCache(UNIPROTKB_EH_CACHE_NAME));
        return new UniProtKBStoreClient(uniProtKBEntryStore);
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
