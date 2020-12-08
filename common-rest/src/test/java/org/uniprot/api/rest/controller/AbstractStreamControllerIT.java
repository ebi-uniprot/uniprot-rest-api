package org.uniprot.api.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.DefaultUriBuilderFactory;
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

    protected static final String SAMPLE_RDF =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF>\n"
                    + "    <owl:Ontology rdf:about=\"\">\n"
                    + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "    </owl:Ontology>\n"
                    + "    <sample>text</sample>\n"
                    + "    <anotherSample>text2</anotherSample>\n"
                    + "    <someMore>text3</someMore>\n"
                    + "</rdf:RDF>";

    @Autowired private RestTemplate restTemplate;

    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;

    private MiniSolrCloudCluster cluster;

    private Path tempClusterDir;

    protected CloudSolrClient cloudSolrClient;

    @BeforeAll
    public void startCluster() throws Exception {
        Properties solrProperties = loadSolrProperties();
        String solrHome = solrProperties.getProperty("solr.home");
        tempClusterDir = Files.createTempDirectory("MiniSolrCloudCluster");
        System.setProperty(
                "solr.data.home", tempClusterDir.toString() + File.separator + "solrTestData");

        JettyConfig jettyConfig = JettyConfig.builder().setPort(0).stopAtShutdown(true).build();
        try {
            cluster = new MiniSolrCloudCluster(1, tempClusterDir, jettyConfig);
            tupleStreamTemplate.getStreamConfig().setZkHost(cluster.getZkServer().getZkAddress());
            ReflectionTestUtils.setField(
                    tupleStreamTemplate, "httpClient", cluster.getSolrClient().getHttpClient());
            ReflectionTestUtils.setField(tupleStreamTemplate, "streamFactory", null);
            ReflectionTestUtils.setField(tupleStreamTemplate, "streamContext", null);
            cloudSolrClient = cluster.getSolrClient();

            updateFacetTupleStreamTemplate();

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

        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
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
            cloudSolrClient = null;
        }
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
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

    private void updateFacetTupleStreamTemplate() {
        // update facet tuple for fields value for testing
        ReflectionTestUtils.setField(
                facetTupleStreamTemplate, "zookeeperHost", cluster.getZkServer().getZkAddress());
        ReflectionTestUtils.setField(
                facetTupleStreamTemplate, "httpClient", cluster.getSolrClient().getHttpClient());
        ReflectionTestUtils.setField(facetTupleStreamTemplate, "streamFactory", null);
        ReflectionTestUtils.setField(facetTupleStreamTemplate, "streamContext", null);
    }
}
