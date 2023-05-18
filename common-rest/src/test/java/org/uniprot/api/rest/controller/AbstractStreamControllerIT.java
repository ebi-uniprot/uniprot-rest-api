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
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.store.search.SolrCollection;

/**
 * @author lgonzales
 * @since 17/06/2020
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractStreamControllerIT {

    private static final String SOLR_SYSTEM_PROPERTIES = "solr-system.properties";

    public static final String SAMPLE_RDF =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF>\n"
                    + "    <owl:Ontology rdf:about=\"\">\n"
                    + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "    </owl:Ontology>\n"
                    + "    <sample>text</sample>\n"
                    + "    <anotherSample>text2</anotherSample>\n"
                    + "    <someMore>text3</someMore>\n"
                    + "</rdf:RDF>";

    public static final String SAMPLE_N_TRIPLES =
            "<http://purl.uniprot.org/uniprot/SAMPLE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.uniprot.org/core/SAMPLE> .";

    public static final String SAMPLE_TTL =
            "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix uniref: <http://purl.uniprot.org/uniref/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                    + "<SAMPLE> rdf:type up:Protein ;";

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
                "solr.data.home", tempClusterDir.toString() + File.separator + "solrTestData");

        JettyConfig jettyConfig = JettyConfig.builder().setPort(0).stopAtShutdown(true).build();
        try {
            cluster = new MiniSolrCloudCluster(1, tempClusterDir, jettyConfig);
            getTupleStreamTemplate()
                    .getStreamConfig()
                    .setZkHost(cluster.getZkServer().getZkAddress());
            ReflectionTestUtils.setField(getTupleStreamTemplate(), "streamFactory", null);
            ReflectionTestUtils.setField(getTupleStreamTemplate(), "streamContext", null);
            cloudSolrClient = cluster.getSolrClient();
            ReflectionTestUtils.setField(getTupleStreamTemplate(), "solrClient", cloudSolrClient);
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
    }

    protected abstract List<SolrCollection> getSolrCollections();

    protected abstract TupleStreamTemplate getTupleStreamTemplate();

    protected abstract FacetTupleStreamTemplate getFacetTupleStreamTemplate();

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
                getFacetTupleStreamTemplate(),
                "zookeeperHost",
                cluster.getZkServer().getZkAddress());
        ReflectionTestUtils.setField(getFacetTupleStreamTemplate(), "streamFactory", null);
        ReflectionTestUtils.setField(getFacetTupleStreamTemplate(), "streamContext", null);
    }
}
