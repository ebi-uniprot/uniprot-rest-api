package org.uniprot.api.support.data.taxonomy.response;

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
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyRepository;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class TaxonomyStreamConfig {

    @Bean(name = "taxonomyRDFStreamer")
    public RDFStreamer taxonomyRDFStreamer(
            @Qualifier("taxonomyRDFRestTemplate") RestTemplate restTemplate,
            DefaultDocumentIdStream<TaxonomyDocument> taxonomyDocumentIdStream,
            RDFStreamerConfigProperties taxonomyRDFConfigProperties) {

        RetryPolicy<Object> rdfRetryPolicy =
                RDFStreamConfig.rdfRetryPolicy(taxonomyRDFConfigProperties);

        return RDFStreamer.builder()
                .rdfBatchSize(taxonomyRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.TAXONOMY_PROLOG)
                .idStream(taxonomyDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "taxonomy.streamer.rdf")
    public RDFStreamerConfigProperties taxonomyRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public DefaultDocumentIdStream<TaxonomyDocument> taxonomyDocumentIdStream(
            TaxonomyRepository repository) {
        return DefaultDocumentIdStream.<TaxonomyDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }

    @Bean(name = "taxonomyRDFRestTemplate")
    @Profile("live")
    RestTemplate taxonomyRDFRestTemplate(RDFStreamerConfigProperties taxonomyRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(taxonomyRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
