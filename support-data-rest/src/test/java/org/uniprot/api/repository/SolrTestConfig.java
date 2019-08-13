package org.uniprot.api.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.CoreContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.server.support.EmbeddedSolrServerFactory;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.indexer.ClosableEmbeddedSolrClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.mockito.Mockito.mock;

//@TestConfiguration
@Slf4j
public class SolrTestConfig implements DisposableBean {
    private static final String SOLR_DATA_DIR = "solr.data.dir";
    private static final String TEMP_DIR_PREFIX = "test-solr-data-dir";
    private final File file;

    @Value(("${solr.home}"))
    private String solrHome;

    public SolrTestConfig() throws Exception {
        file = Files.createTempDirectory(TEMP_DIR_PREFIX).toFile();
    }

//    @Bean
//    @Profile("offline")
//    public SolrClient uniProtSolrClient() throws Exception {
//        System.setProperty(SOLR_DATA_DIR, file.getAbsolutePath());
//        EmbeddedSolrServerFactory factory = new EmbeddedSolrServerFactory(solrHome);
//        return factory.getSolrClient();
//    }

//    @Bean
//    @Profile("offline")
//    public SolrTemplate solrTemplate(SolrClient uniProtSolrClient) {
//        return new SolrTemplate(uniProtSolrClient);
//    }

    

    

//    @Bean
//    @Profile("offline")
//    public SolrRequestConverter requestConverter() {
//        return new SolrRequestConverter() {
//            @Override
//            public SolrQuery toSolrQuery(SolrRequest request) {
//                SolrQuery solrQuery = super.toSolrQuery(request);
//
//                // required for tests, because EmbeddedSolrServer is not sharded
//                solrQuery.setParam("distrib", "false");
//                solrQuery.setParam("terms.mincount", "1");
//
//                return solrQuery;
//            }
//        };
//    }

    @Override
    public void destroy() throws Exception {
        if (file != null) {
            FileUtils.deleteDirectory(file);
            log.info("Deleted solr home");
        }
    }

//    @Bean
//    @Profile("offline")
//    public HttpClient httpClient() {
//        return mock(HttpClient.class);
//    }
}
