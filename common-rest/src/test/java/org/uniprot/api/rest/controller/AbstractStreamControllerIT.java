package org.uniprot.api.rest.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.AbstractZkTestCase;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkConfigManager;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.store.search.SolrCollection;

import lombok.extern.slf4j.Slf4j;

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
            "<http://purl.uniprot.org/uniprot/P05067> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.uniprot.org/core/Protein> .\n"
                    + "<http://purl.uniprot.org/uniprot/P05067> <http://purl.uniprot.org/core/reviewed> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> .\n"
                    + "<http://purl.uniprot.org/uniprot/P05067> <http://purl.uniprot.org/core/created> \"1987-08-13\"^^<http://www.w3.org/2001/XMLSchema#date> .\n"
                    + "<http://purl.uniprot.org/uniprot/P05067> <http://purl.uniprot.org/core/modified> \"2025-04-09\"^^<http://www.w3.org/2001/XMLSchema#date> .";

    public static final String SAMPLE_TTL =
            "@base <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix uniref: <http://purl.uniprot.org/uniref/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                    + "value1 value2 value3 .\n"
                    + "anothervalue1 anothervalue2 anothervalue3 .";

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private static final Object LOCK = new Object();

    // TODO to be removed
    protected CloudSolrClient cloudSolrClient;

    @BeforeAll
    public void initializeSolrCluster() throws Exception {
        Properties solrProperties = loadSolrProperties();
        String solrHome = solrProperties.getProperty("solr.home");
        String zkHost = System.getProperty("spring.data.solr.zkHost");

        try {
            for (SolrCollection solrCollection : getSolrCollections()) {
                String collection = solrCollection.name();
                Path configPath = Paths.get(solrHome + File.separator + collection + "/conf");

                uploadConfigs(zkHost, configPath, collection);
                synchronized (LOCK) {
                    setupCollection(collection);
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize collections for testcontainer based solr", e);
            throw e;
        }
    }

    private void setupCollection(String collection) throws SolrServerException, IOException {
        deleteCollectionIfExists(collection);
        CollectionAdminRequest.createCollection(collection, collection, 1, 1)
                .process(getSolrClient());
    }

    private static void uploadConfigs(String zkHost, Path configPath, String collection)
            throws KeeperException, InterruptedException, IOException {
        try (SolrZkClient zkClient =
                new SolrZkClient(
                        zkHost, AbstractZkTestCase.TIMEOUT, AbstractZkTestCase.TIMEOUT, null)) {

            /*
                This is live nodes logging is purely for debugging purposes. Could be removed one itests with
                solr testcontainers are stable.
            */
            List<String> liveNodes = zkClient.getChildren("/live_nodes", null, true);
            liveNodes.forEach(n -> log.info("live_node: {}", n));

            ZkConfigManager manager = new ZkConfigManager(zkClient);
            manager.uploadConfigDir(configPath, collection);
        }
    }

    private void deleteCollectionIfExists(String collection) {
        try {
            CollectionAdminRequest.deleteCollection(collection).process(getSolrClient());
            log.info("Deleted existing collection '{}'", collection);
        } catch (Exception e) {
            // Solr throws if the collection doesn't exist yet
            log.debug("Collection '{}' did not exist, nothing to delete", collection);
        }
    }

    protected abstract List<SolrCollection> getSolrCollections();

    protected abstract TupleStreamTemplate getTupleStreamTemplate();

    protected abstract FacetTupleStreamTemplate getFacetTupleStreamTemplate();

    protected SolrClient getSolrClient() {
        /*
            TODO keeping default implementation to return null to prevent compile errors for other backends
             as only uniprotkb-rest is updated so far. This can later be converted to an abstract method.
         */
        return null;
    }

    protected Stream<Arguments> getContentTypes(String requestPath) {
        return ControllerITUtils.getContentTypes(requestPath, requestMappingHandlerMapping).stream()
                .map(Arguments::of);
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
