package org.uniprot.api.uniref.controller;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniref.UniRefDocument;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractUniRefDownloadIT extends AbstractStreamControllerIT {
    static RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    public static void setUpThings(DynamicPropertyRegistry propertyRegistry) {
        Startables.deepStart(rabbitMQContainer, redisContainer).join();
        assertTrue(rabbitMQContainer.isRunning());
        assertTrue(redisContainer.isRunning());
        propertyRegistry.add("spring.amqp.rabbit.port", rabbitMQContainer::getFirstMappedPort);
        propertyRegistry.add("spring.amqp.rabbit.host", rabbitMQContainer::getHost);
        System.setProperty("uniprot.redis.host", redisContainer.getHost());
        System.setProperty(
                "uniprot.redis.port", String.valueOf(redisContainer.getFirstMappedPort()));
        propertyRegistry.add("ALLOW_EMPTY_PASSWORD", () -> "yes");
    }

    @Value("${download.idFilesFolder}")
    protected String idsFolder;

    @Value("${download.resultFilesFolder}")
    protected String resultFolder;

    @Value("${async.download.queueName}")
    protected String downloadQueue;

    @Value("${async.download.retryQueueName}")
    protected String retryQueue;

    @Value(("${async.download.rejectedQueueName}"))
    protected String rejectedQueue;

    @Autowired private UniRefQueryRepository unirefQueryRepository;

    private final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    @Autowired private SolrClient solrClient;

    @Autowired private TaxonomyLineageRepository taxRepository;

    @Autowired protected AmqpAdmin amqpAdmin;

    @Autowired
    private UniProtStoreClient<UniRefEntryLight> storeClient; // in memory voldemort store client

    @MockBean(name = "uniProtRdfRestTemplate")
    private RestTemplate restTemplate;

    @BeforeAll
    public void saveEntriesInSolrAndStore() throws Exception {
        ReflectionTestUtils.setField(unirefQueryRepository, "solrClient", cloudSolrClient);
        prepareDownloadFolders();
        saveEntries();

        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);

        ReflectionTestUtils.setField(taxRepository, "solrClient", cloudSolrClient);
    }

    @BeforeEach
    void setUp() {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    private void prepareDownloadFolders() throws IOException {
        Files.createDirectories(Path.of(this.idsFolder));
        Files.createDirectories(Path.of(this.resultFolder));
    }

    @AfterAll
    public void cleanUpData() throws Exception {
        cleanUpFolder(this.idsFolder);
        cleanUpFolder(this.resultFolder);
        getDownloadJobRepository().deleteAll();
        this.amqpAdmin.purgeQueue(rejectedQueue, true);
        this.amqpAdmin.purgeQueue(downloadQueue, true);
        this.amqpAdmin.purgeQueue(retryQueue, true);
        rabbitMQContainer.stop();
        redisContainer.stop();
    }

    protected abstract DownloadJobRepository getDownloadJobRepository();

    private void cleanUpFolder(String folder) throws IOException {
        Files.list(Path.of(folder))
                .forEach(
                        path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 4; i++) {
            saveEntry(i, UniRefType.UniRef50);
            saveEntry(i, UniRefType.UniRef90);
            saveEntry(i, UniRefType.UniRef100);
        }
        cloudSolrClient.commit(SolrCollection.uniref.name());
    }

    private void saveEntry(int i, UniRefType type) throws Exception {
        UniRefEntry entry = UniRefEntryMocker.createEntry(i, type);
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        UniRefDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniref.name(), doc);
        storeClient.saveEntry(entryLight);
    }

    private void saveTaxonomyEntry(long taxId) throws Exception {
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .rank(TaxonomyRank.SPECIES)
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 1).build())
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 2).build())
                        .build();
        byte[] taxonomyObj =
                TaxonomyJsonConfig.getInstance()
                        .getFullObjectMapper()
                        .writeValueAsBytes(taxonomyEntry);

        TaxonomyDocument.TaxonomyDocumentBuilder docBuilder =
                TaxonomyDocument.builder()
                        .taxId(taxId)
                        .id(String.valueOf(taxId))
                        .taxonomyObj(taxonomyObj);
        cloudSolrClient.addBean(SolrCollection.taxonomy.name(), docBuilder.build());
    }
}
