package org.uniprot.api.support.data.literature.response;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.support.data.common.RDFStreamConfig;
import org.uniprot.api.support.data.common.SolrDocumentRDFIdConverter;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class LiteratureStreamConfig {

    @Bean(name = "literatureRDFStreamer")
    public RDFStreamer literatureRDFStreamer(
            @Qualifier("literatureRDFRestTemplate") RestTemplate restTemplate,
            DefaultDocumentIdStream<LiteratureDocument> literatureDocumentIdStream,
            RDFStreamerConfigProperties literatureRDFConfigProperties) {

        RetryPolicy<Object> rdfRetryPolicy =
                RDFStreamConfig.rdfRetryPolicy(literatureRDFConfigProperties);

        return RDFStreamer.builder()
                .rdfBatchSize(literatureRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.LITERATURE_PROLOG)
                .idStream(literatureDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "literature.streamer.rdf")
    public RDFStreamerConfigProperties literatureRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public DefaultDocumentIdStream<LiteratureDocument> literatureDocumentIdStream(
            LiteratureRepository repository) {
        return DefaultDocumentIdStream.<LiteratureDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }

    @Bean(name = "literatureRDFRestTemplate")
    @Profile("live")
    RestTemplate literatureRDFRestTemplate(
            RDFStreamerConfigProperties literatureRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(literatureRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
