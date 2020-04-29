package org.uniprot.api.rest.request;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

import static org.mockito.Mockito.mock;

/**
 * Created 29/04/2020
 *
 * @author Edd
 */
@ActiveProfiles("offline")
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, FakeRESTMain.UnusedBeanMocker.class})
@ComponentScan("org.uniprot.api.rest")
public class FakeRESTMain {
    public static void main(String[] args) {
        SpringApplication.run(FakeRESTMain.class, args);
    }

    @Configuration
    static class UnusedBeanMocker {
        @Primary
        @Bean
        public SolrClient solrClient() {
            return mock(SolrClient.class);
        }
    }
}
