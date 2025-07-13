package org.uniprot.api.async.download.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.createEntry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.async.download.controller.IdMappingAsyncConfig;
import org.uniprot.api.async.download.controller.validator.UniParcIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.controller.validator.UniProtKBIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.controller.validator.UniRefIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIdMappingIT {
    @Autowired protected IdMappingAsyncConfig idMappingAsyncConfig;
    @Autowired private AmqpAdmin amqpAdmin;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate idMappingRdfRestTemplate;

    private static final UniProtKBEntry TEMPLATE_KB_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
    @Autowired protected IdMappingJobCacheService cacheService;

    @Autowired private UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient;

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Autowired private UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient;
    @Autowired protected IdMappingDownloadJobRepository downloadJobRepository;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    static final RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"))
                    .withExposedPorts(5672, 15672)
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withTmpFs(Map.of("/var/lib/rabbitmq", "rw"));

    static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void setUp(DynamicPropertyRegistry propertyRegistry) {
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

    @BeforeAll
    public void setUpDownload() throws Exception {
        Duration asyncDuration = Duration.ofMillis(500);
        Awaitility.setDefaultPollDelay(asyncDuration);
        Awaitility.setDefaultPollInterval(asyncDuration);
        Files.createDirectories(Path.of(idMappingAsyncConfig.getIdsFolder()));
        Files.createDirectories(Path.of(idMappingAsyncConfig.getResultFolder()));

        for (int i = 1; i <= 10; i++) {
            saveUniParc(i);
            saveUniProtKB(i, "");

            saveUniRef(createEntry(i, UniRefType.UniRef50));
            saveUniRef(createEntry(i, UniRefType.UniRef90));
            saveUniRef(createEntry(i, UniRefType.UniRef100));
        }
        saveUniProtKB(11, "-2");
        saveUniProtKB(12, "-2");
    }

    @BeforeEach
    public void setUpEach() {
        DefaultUriBuilderFactory handler = Mockito.mock(DefaultUriBuilderFactory.class);
        when(idMappingRdfRestTemplate.getUriTemplateHandler()).thenReturn(handler);

        UriBuilder uriBuilder = Mockito.mock(UriBuilder.class);
        when(handler.builder()).thenReturn(uriBuilder);

        URI uniProtRdfServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniprotkb"), eq("rdf"), any())).thenReturn(uniProtRdfServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniProtRdfServiceURI), any()))
                .thenReturn(
                        "<?xml version='1.0' encoding='UTF-8'?>\n"
                                + "<rdf:RDF>\n"
                                + "    <owl:Ontology rdf:about=\"\">\n"
                                + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                                + "    </owl:Ontology>\n"
                                + "    <sample>text</sample>\n"
                                + "    <rdf:Description rdf:about=\"P00001\">\n"
                                + "    <rdf:Description rdf:about=\"P00002\">\n"
                                + "</rdf:RDF>");

        URI uniParcRdfServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniparc"), eq("rdf"), any())).thenReturn(uniParcRdfServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniParcRdfServiceURI), any()))
                .thenReturn(
                        "<?xml version='1.0' encoding='UTF-8'?>\n"
                                + "<rdf:RDF>\n"
                                + "    <owl:Ontology rdf:about=\"\">\n"
                                + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                                + "    </owl:Ontology>\n"
                                + "    <sample>text</sample>\n"
                                + "    <rdf:Description rdf:about=\"UPI000012A72A\">\n"
                                + "    <rdf:Description rdf:about=\"UPI000012A73A\">\n"
                                + "</rdf:RDF>");

        URI uniRefRdfServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniref"), eq("rdf"), any())).thenReturn(uniRefRdfServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniRefRdfServiceURI), any()))
                .thenReturn(
                        "<?xml version='1.0' encoding='UTF-8'?>\n"
                                + "<rdf:RDF>\n"
                                + "    <owl:Ontology rdf:about=\"\">\n"
                                + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                                + "    </owl:Ontology>\n"
                                + "    <sample>text</sample>\n"
                                + "    <rdf:Description rdf:about=\"UniRef100_P21802\">\n"
                                + "    <rdf:Description rdf:about=\"UniRef100_P21803\">\n"
                                + "</rdf:RDF>");

        URI uniProtNtServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniprotkb"), eq("nt"), any())).thenReturn(uniProtNtServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniProtNtServiceURI), any()))
                .thenReturn(
                        "<http://purl.uniprot.org/uniprot/SAMPLE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.uniprot.org/core/SAMPLE> .\n"
                                + "<http://purl.uniprot.org/uniprot/P00001> <http://purl.uniprot.org/core/reviewed>\n"
                                + "<http://purl.uniprot.org/uniprot/P00002> <http://purl.uniprot.org/core/reviewed>");

        URI uniParcNtServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniparc"), eq("nt"), any())).thenReturn(uniParcNtServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniParcNtServiceURI), any()))
                .thenReturn(
                        "<http://purl.uniprot.org/uniprot/SAMPLE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.uniprot.org/core/SAMPLE> .\n"
                                + "<http://purl.uniprot.org/uniprot/UPI000012A71A> <http://purl.uniprot.org/core/reviewed>\n"
                                + "<http://purl.uniprot.org/uniprot/UPI000012A72A> <http://purl.uniprot.org/core/reviewed>");

        URI uniRefNtServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniref"), eq("nt"), any())).thenReturn(uniRefNtServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniRefNtServiceURI), any()))
                .thenReturn(
                        "<http://purl.uniprot.org/uniprot/SAMPLE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.uniprot.org/core/SAMPLE> .\n"
                                + "<http://purl.uniprot.org/uniprot/UniRef100_P21801> <http://purl.uniprot.org/core/reviewed>\n"
                                + "<http://purl.uniprot.org/uniprot/UniRef100_P21802> <http://purl.uniprot.org/core/reviewed>");

        URI uniProtTtlServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniprotkb"), eq("ttl"), any())).thenReturn(uniProtTtlServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniProtTtlServiceURI), any()))
                .thenReturn(
                        "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                                + "<P00001> rdf:type up:Protein ;\n"
                                + "<P00002> rdf:type up:Protein ;\n"
                                + "<SAMPLE> rdf:type up:Protein ;");

        URI uniParcTtlServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniparc"), eq("ttl"), any())).thenReturn(uniParcTtlServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniParcTtlServiceURI), any()))
                .thenReturn(
                        "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                                + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                                + "<UPI000012A72A> rdf:type up:Protein ;\n"
                                + "<UPI000012A73A> rdf:type up:Protein ;\n"
                                + "<SAMPLE> rdf:type up:Protein ;");

        URI uniRefTtlServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniref"), eq("ttl"), any())).thenReturn(uniRefTtlServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniRefTtlServiceURI), any()))
                .thenReturn(
                        "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                                + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                                + "<UniRef100_P21802> rdf:type up:Protein ;\n"
                                + "<UniRef100_P21803> rdf:type up:Protein ;\n"
                                + "<SAMPLE> rdf:type up:Protein ;");
    }

    private void saveUniProtKB(int i, String isoFormString) {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_KB_ENTRY);
        String acc = String.format("P%05d", i) + isoFormString;
        entryBuilder.primaryAccession(acc);

        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        uniProtKBStoreClient.saveEntry(uniProtKBEntry);
    }

    private void saveUniParc(int i) {
        UniParcEntry uniParcEntry = UniParcEntryMocker.createUniParcEntry(i, "UPI0000283A");
        UniParcEntryLight uniParcLightEntry =
                UniParcEntryMocker.convertToUniParcEntryLight(uniParcEntry);
        uniParcLightStoreClient.saveEntry(uniParcLightEntry);
        // save cross references in store
        String xrefBatchKey = uniParcLightEntry.getUniParcId() + "_0";
        uniParcCrossReferenceStoreClient.saveEntry(
                new UniParcCrossReferencePair(
                        xrefBatchKey, uniParcEntry.getUniParcCrossReferences()));
    }

    private void saveUniRef(UniRefEntry uniRefEntry) {
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(uniRefEntry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        uniRefStoreClient.saveEntry(entryLight);
    }

    @AfterEach
    void cleanUpRedisAndFiles() throws IOException {
        cleanUpFolder(idMappingAsyncConfig.getIdsFolder());
        cleanUpFolder(idMappingAsyncConfig.getResultFolder());
        downloadJobRepository.deleteAll();
    }

    @AfterAll
    public void cleanUpData() throws IOException {
        cleanUpFolder(idMappingAsyncConfig.getIdsFolder());
        cleanUpFolder(idMappingAsyncConfig.getResultFolder());
        downloadJobRepository.deleteAll();
        this.amqpAdmin.purgeQueue(idMappingAsyncConfig.getRejectedQueue(), true);
        this.amqpAdmin.purgeQueue(idMappingAsyncConfig.getDownloadQueue(), true);
        this.amqpAdmin.purgeQueue(idMappingAsyncConfig.getRetryQueue(), true);
        this.rabbitMQContainer.stop();
        this.redisContainer.stop();
    }

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

    protected String cleanFormat(String format) {
        StringBuilder result = new StringBuilder();
        for (char c : format.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    protected void cacheIdMappingJob(
            String jobId, String to, JobStatus jobStatus, List<IdMappingStringPair> mappedIds) {
        cacheIdMappingJob(jobId, to, jobStatus, mappedIds, List.of());
    }

    protected void cacheIdMappingJob(
            String jobId,
            String to,
            JobStatus jobStatus,
            List<IdMappingStringPair> mappedIds,
            List<String> unMappedId) {
        IdMappingJobRequest idMappingRequest = new IdMappingJobRequest();
        idMappingRequest.setTo(to);

        IdMappingResult idMappingResult =
                IdMappingResult.builder().mappedIds(mappedIds).unmappedIds(unMappedId).build();

        IdMappingJob idmappingJob =
                IdMappingJob.builder()
                        .jobStatus(jobStatus)
                        .idMappingRequest(idMappingRequest)
                        .idMappingResult(idMappingResult)
                        .build();
        cacheService.put(jobId, idmappingJob);
    }

    protected Stream<Arguments> getAllUniProtKBFormats() {
        return UniProtKBIdMappingDownloadRequestValidator.VALID_FORMATS.stream().map(Arguments::of);
    }

    protected Stream<Arguments> getAllUniRefFormats() {
        return UniRefIdMappingDownloadRequestValidator.VALID_FORMATS.stream().map(Arguments::of);
    }

    protected Stream<Arguments> getAllUniParcFormats() {
        return UniParcIdMappingDownloadRequestValidator.VALID_FORMATS.stream().map(Arguments::of);
    }

    protected void validateSupportedRDFMediaTypes(String format, String text) {
        switch (format) {
            case UniProtMediaType.TURTLE_MEDIA_TYPE_VALUE:
                assertTrue(text.contains("@prefix xsd: <http://www.w3.org/2001/XMLSchema#>"));
                assertTrue(text.contains("<SAMPLE> rdf:type up:Protein ;"));
                break;
            case UniProtMediaType.N_TRIPLES_MEDIA_TYPE_VALUE:
                assertTrue(text.contains("<http://purl.uniprot.org/uniprot/SAMPLE>"));
                break;
            case UniProtMediaType.RDF_MEDIA_TYPE_VALUE:
                assertTrue(text.contains("<rdf:RDF"));
                assertTrue(text.contains("<sample>text</sample>"));
                assertTrue(text.contains("</rdf:RDF>"));
                break;
            default:
                fail("Invalid SUPPORTED_RDF_MEDIA_TYPES");
        }
    }
}
