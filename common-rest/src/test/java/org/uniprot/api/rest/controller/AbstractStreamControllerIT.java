package org.uniprot.api.rest.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.store.TupleStreamTemplate;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 17/06/2020
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractStreamControllerIT {

    private static final String SOLR_SYSTEM_PROPERTIES = "solr-system.properties";

    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private MiniSolrCloudCluster cluster;

    private Path tempClusterDir;

    protected CloudSolrClient cloudSolrClient;

    @BeforeAll
    public void startCluster() throws Exception {
        Properties solrProperties = loadSolrProperties();
        String solrHome = solrProperties.getProperty("solr.home");
        tempClusterDir = Files.createTempDirectory("MiniSolrCloudCluster");
        System.setProperty(
                "solr.data.dir", tempClusterDir.toString() + File.separator + "solrTestData");

        JettyConfig jettyConfig = JettyConfig.builder().setPort(0).build();
        try {
            cluster = new MiniSolrCloudCluster(1, tempClusterDir, jettyConfig);
            tupleStreamTemplate.getStreamConfig().setZkHost(cluster.getZkServer().getZkAddress());
            ReflectionTestUtils.setField(
                    tupleStreamTemplate, "httpClient", cluster.getSolrClient().getHttpClient());
            cloudSolrClient = cluster.getSolrClient();

            // update facet tuple for fields value for testing
            ReflectionTestUtils.setField(facetTupleStreamTemplate, "zookeeperHost", cluster.getZkServer().getZkAddress());
            ReflectionTestUtils.setField(facetTupleStreamTemplate, "httpClient", cluster.getSolrClient().getHttpClient());

            for (SolrCollection solrCollection : getSolrCollections()) {
                String collection = solrCollection.name();
                Path configPath = Paths.get(solrHome + File.separator + collection + "/conf");
                cluster.uploadConfigSet(configPath, collection);
                CollectionAdminRequest.createCollection(collection, collection, 1, 1)
                        .process(cloudSolrClient);
            }
        } catch (Exception exc) {
            log.error("Failed to initialize a MiniSolrCloudCluster due to: " + exc, exc);
            throw exc;
        }
    }

    protected abstract List<SolrCollection> getSolrCollections();

    protected Stream<Arguments> getContentTypes(String requestPath) {
        return ControllerITUtils.getContentTypes(requestPath, requestMappingHandlerMapping).stream()
                .map(Arguments::of);
    }

    @AfterAll
    public void stopCluster() throws Exception {
        if (cloudSolrClient != null) {
            cloudSolrClient.close();
        }
        if (cluster != null) {
            cluster.shutdown();
        }

        // Delete tempDir content
        FileSystemUtils.deleteRecursively(tempClusterDir);
    }

    private Properties loadSolrProperties() throws IOException {
        Properties properties = new Properties();
        InputStream propertiesStream =
                AbstractStreamControllerIT.class
                        .getClassLoader()
                        .getResourceAsStream(SOLR_SYSTEM_PROPERTIES);
        properties.load(propertiesStream);
        return properties;
    }
}
