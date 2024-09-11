package org.uniprot.api.async.download.messaging.consumer.idmapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.async.download.messaging.consumer.MessageConsumer.CURRENT_RETRIED_COUNT_HEADER;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_N_TRIPLES;
import static org.uniprot.api.rest.download.model.JobStatus.*;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.createEntry;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.controller.IdMappingAsyncConfig;
import org.uniprot.api.async.download.controller.validator.UniParcIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.controller.validator.UniProtKBIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.controller.validator.UniRefIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingFileHandler;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {TestConfig.class, AsyncDownloadRestApp.class})
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Testcontainers
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdMappingMessageConsumerIT {
    private static final String ID = "someId";
    private static final String JOB_ID_HEADER = "jobId";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long UPDATE_COUNT = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long PROCESSED_ENTRIES = 23;

    @Autowired private IdMappingAsyncConfig idMappingAsyncConfig;

    @Autowired private AmqpAdmin amqpAdmin;

    @Autowired private IdMappingDownloadJobRepository downloadJobRepository;

    @Autowired private IdMappingJobCacheService cacheService;

    @Autowired private IdMappingDownloadConfigProperties downloadConfigProperties;

    @Autowired private UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient;

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Autowired private IdMappingFileHandler asyncDownloadFileHandler;

    @Autowired private IdMappingMessageConsumer idMappingMessageConsumer;

    @Autowired private UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate idMappingRdfRestTemplate;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    @Autowired private MessageConverter messageConverter;

    private static final UniProtKBEntry TEMPLATE_KB_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    @Container
    private static final RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    @Container
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    @Autowired private IdMappingDownloadJobRepository idMappingDownloadJobRepository;

    @DynamicPropertySource
    public static void setUp(DynamicPropertyRegistry propertyRegistry) {
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
                                + "<UPI000012A72A> rdf:type up:Protein ;\n"
                                + "<UPI000012A73A> rdf:type up:Protein ;\n"
                                + "<SAMPLE> rdf:type up:Protein ;");

        URI uniRefTtlServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq("uniref"), eq("ttl"), any())).thenReturn(uniRefTtlServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(uniRefTtlServiceURI), any()))
                .thenReturn(
                        "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                                + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
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
        xrefStoreClient.saveEntry(
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
    void tearDown() {
        asyncDownloadFileHandler.deleteAllFiles(ID);
    }

    @AfterAll
    public void cleanUpData() throws Exception {
        cleanUpFolder(idMappingAsyncConfig.getIdsFolder());
        cleanUpFolder(idMappingAsyncConfig.getResultFolder());
        downloadJobRepository.deleteAll();
        this.amqpAdmin.purgeQueue(idMappingAsyncConfig.getRejectedQueue(), true);
        this.amqpAdmin.purgeQueue(idMappingAsyncConfig.getDownloadQueue(), true);
        this.amqpAdmin.purgeQueue(idMappingAsyncConfig.getRetryQueue(), true);
        rabbitMQContainer.stop();
        redisContainer.stop();
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
                        .jobId(jobId)
                        .build();
        cacheService.put(jobId, idmappingJob);
    }

    @Test
    void onMessage_fordUniParcWithJsonFinishedSuccessfully() throws Exception {
        String idMappingId = "aac4dca9f543088cce4b400aed297ed115b64af1";
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        List<String> unMappedIds = List.of("UPI0000283100", "UPI0000283200");
        cacheIdMappingJob(idMappingId, "UniParc", JobStatus.FINISHED, mappedIds, unMappedIds);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        IdMappingDownloadRequest downloadRequest = new IdMappingDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setIdMappingJobId(idMappingId);
        downloadRequest.setFields("upi,organism");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob downloadJob = downloadJobRepository.findById(ID).get();
        assertDownloadJobSuccess(downloadJob);
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                MAPPER.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(2, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P10001", "P10002")));
        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("uniParcId").asText())
                        .collect(Collectors.toSet())
                        .containsAll(List.of("UPI0000283A01", "UPI0000283A02")));
        assertTrue(
                to.stream()
                        .map(node -> node.findPath("sequence"))
                        .filter(node -> !(node instanceof MissingNode))
                        .collect(Collectors.toSet())
                        .isEmpty());
        ArrayNode failed = (ArrayNode) jsonResult.findPath("failedIds");
        List<String> failedIds = new ArrayList<>();
        failed.forEach(node -> failedIds.add(node.asText()));
        assertEquals(unMappedIds, failedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniParcFormats")
    void onMessage_fordUniParcWithAllFormatsFinishedSuccessfully(String format) throws Exception {
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        String idMappingId = "UNIPARC_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(idMappingId, "UniParc", JobStatus.FINISHED, mappedIds);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        IdMappingDownloadRequest downloadRequest = new IdMappingDownloadRequest();
        downloadRequest.setFormat(format);
        downloadRequest.setIdMappingJobId(idMappingId);
        downloadRequest.setFields("upi,organism");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob downloadJob = downloadJobRepository.findById(ID).get();
        assertDownloadJobSuccess(downloadJob);
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        InputStream inputStream =
                new GzipCompressorInputStream(new FileInputStream(resultFilePath.toFile()));
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        MediaType mediaType = UniProtMediaType.valueOf(format);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(mediaType)) {
            validateSupportedRDFMediaTypes(format, text);
        } else {
            assertTrue(text.contains("UPI0000283A01"));
            assertTrue(text.contains("UPI0000283A02"));
        }
    }

    private void assertDownloadJobSuccess(IdMappingDownloadJob downloadJob) {
        assertEquals(ID, downloadJob.getId());
        assertEquals(JobStatus.FINISHED, downloadJob.getStatus());
        assertEquals(2, downloadJob.getTotalEntries());
        assertEquals(2, downloadJob.getProcessedEntries());
        assertEquals(1, downloadJob.getUpdateCount());
        assertEquals(ID, downloadJob.getResultFile());
    }

    @Test
    void onMessage_fordUniRefWithJsonFinishedSuccessfully() throws Exception {
        String idMappingId = "441c3ff4b48904378071ce766b0973c94f17d8fd";
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef90_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef90_P03902"));
        List<String> unMappedIds = List.of("UniRef90_P03001", "UniRef90_P03002");
        cacheIdMappingJob(idMappingId, "UniRef90", JobStatus.FINISHED, mappedIds, unMappedIds);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        IdMappingDownloadRequest downloadRequest = new IdMappingDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setIdMappingJobId(idMappingId);
        downloadRequest.setFields("id,name,common_taxon");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob downloadJob = downloadJobRepository.findById(ID).get();
        assertDownloadJobSuccess(downloadJob);
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                MAPPER.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(2, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P10001", "P10002")));
        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("id").asText())
                        .collect(Collectors.toSet())
                        .containsAll(List.of("UniRef90_P03901", "UniRef90_P03902")));
        assertTrue(
                to.stream()
                        .map(node -> node.findPath("members"))
                        .filter(node -> !(node instanceof MissingNode))
                        .collect(Collectors.toSet())
                        .isEmpty());
        ArrayNode failed = (ArrayNode) jsonResult.findPath("failedIds");
        List<String> failedIds = new ArrayList<>();
        failed.forEach(node -> failedIds.add(node.asText()));
        assertEquals(unMappedIds, failedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniRefFormats")
    void onMessage_fordUniRefWithAllFormatsFinishedSuccessfully(String format) throws Exception {
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef50_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef50_P03902"));
        String idMappingId = "UNIREF_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(idMappingId, "UniRef50", JobStatus.FINISHED, mappedIds);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        IdMappingDownloadRequest downloadRequest = new IdMappingDownloadRequest();
        downloadRequest.setFormat(format);
        downloadRequest.setIdMappingJobId(idMappingId);
        downloadRequest.setFields("id,name,common_taxon");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);

        idMappingMessageConsumer.onMessage(message);

        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        InputStream inputStream =
                new GzipCompressorInputStream(new FileInputStream(resultFilePath.toFile()));
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        MediaType mediaType = UniProtMediaType.valueOf(format);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(mediaType)) {
            validateSupportedRDFMediaTypes(format, text);
        } else {
            assertTrue(text.contains("UniRef50_P03901"));
            assertTrue(text.contains("UniRef50_P03902"));
        }
    }

    @Test
    void onMessage_fordUniProtKBWithJsonFinishedSuccessfully() throws Exception {
        String idMappingId = "608f212e45a8054809bd179b803029494d72418c";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        List<String> unMappedIds = List.of("P12345", "P54321");
        cacheIdMappingJob(idMappingId, "UniProtKB", JobStatus.FINISHED, mappedIds, unMappedIds);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        IdMappingDownloadRequest downloadRequest = new IdMappingDownloadRequest();
        downloadRequest.setFormat("json");
        downloadRequest.setIdMappingJobId(idMappingId);
        downloadRequest.setFields("accession,gene_names,organism_name");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob downloadJob = downloadJobRepository.findById(ID).get();
        assertDownloadJobSuccess(downloadJob);
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        JsonNode jsonResult =
                MAPPER.readTree(
                        new GzipCompressorInputStream(
                                new FileInputStream(resultFilePath.toFile())));
        ArrayNode results = (ArrayNode) jsonResult.findPath("results");
        assertEquals(2, results.size());
        assertTrue(results.findValuesAsText("from").containsAll(List.of("P00001", "P00001")));
        List<JsonNode> to = results.findValues("to");
        assertTrue(
                to.stream()
                        .map(node -> node.findValue("primaryAccession").asText())
                        .collect(Collectors.toSet())
                        .containsAll(List.of("P00001", "P00001")));
        assertTrue(
                to.stream()
                        .map(node -> node.findPath("features"))
                        .filter(node -> !(node instanceof MissingNode))
                        .collect(Collectors.toSet())
                        .isEmpty());
        ArrayNode failed = (ArrayNode) jsonResult.findPath("failedIds");
        List<String> failedIds = new ArrayList<>();
        failed.forEach(node -> failedIds.add(node.asText()));
        assertEquals(unMappedIds, failedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniProtKBFormats")
    void onMessage_fordUniProtKBWithAllFormatsFinishedSuccessfully(String format) throws Exception {
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        String idMappingId = "UNIPROTKB_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(idMappingId, "UniProtKB", JobStatus.FINISHED, mappedIds);
        saveDownloadJob(ID, 0, NEW, 0, 0);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        IdMappingDownloadRequest downloadRequest = new IdMappingDownloadRequest();
        downloadRequest.setFormat(format);
        downloadRequest.setIdMappingJobId(idMappingId);
        downloadRequest.setFields("accession,gene_names,organism_name");
        Message message = messageConverter.toMessage(downloadRequest, messageHeader);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob downloadJob = downloadJobRepository.findById(ID).get();
        assertDownloadJobSuccess(downloadJob);
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + ID
                                + FileType.GZIP.getExtension());
        assertTrue(Files.exists(resultFilePath));
        InputStream inputStream =
                new GzipCompressorInputStream(new FileInputStream(resultFilePath.toFile()));
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        MediaType mediaType = UniProtMediaType.valueOf(format);
        if (UniProtMediaType.SUPPORTED_RDF_MEDIA_TYPES.containsKey(mediaType)) {
            validateSupportedRDFMediaTypes(format, text);
        } else {
            assertTrue(text.contains("P00001"));
            assertTrue(text.contains("P00002"));
        }
    }

    @Test
    void onMessage_maxRetriesReached() {
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        messageHeader.setHeader(CURRENT_RETRIED_COUNT_HEADER, MAX_RETRY_COUNT);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, MAX_RETRY_COUNT, ERROR, UPDATE_COUNT, PROCESSED_ENTRIES);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob job = downloadJobRepository.findById(ID).get();
        assertEquals(ERROR, job.getStatus());
        assertEquals(MAX_RETRY_COUNT, job.getRetried());
        assertEquals(0, job.getUpdateCount());
        assertEquals(0, job.getProcessedEntries());
        assertFalse(asyncDownloadFileHandler.areAllFilesPresent(ID));
    }

    @Test
    void onMessage_jobCurrentlyRunning() throws Exception {
        createResultFile(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, 0, RUNNING, UPDATE_COUNT, PROCESSED_ENTRIES);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob job = downloadJobRepository.findById(ID).get();
        assertEquals(RUNNING, job.getStatus());
        assertEquals(0, job.getRetried());
        assertEquals(UPDATE_COUNT, job.getUpdateCount());
        assertTrue(asyncDownloadFileHandler.isResultFilePresent(ID));
    }

    @Test
    void onMessage_alreadyFinished() throws Exception {
        createResultFile(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, 0, FINISHED, UPDATE_COUNT, PROCESSED_ENTRIES);

        idMappingMessageConsumer.onMessage(message);

        IdMappingDownloadJob job = downloadJobRepository.findById(ID).get();
        assertEquals(FINISHED, job.getStatus());
        assertEquals(0, job.getRetried());
        assertEquals(UPDATE_COUNT, job.getUpdateCount());
        assertEquals(PROCESSED_ENTRIES, job.getProcessedEntries());
        assertTrue(asyncDownloadFileHandler.isResultFilePresent(ID));
    }

    @Test
    void onMessage_retryLoop() throws Exception {
        createResultFile(ID);
        MessageProperties messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID_HEADER, ID);
        messageHeader.setHeader(CURRENT_RETRIED_COUNT_HEADER, 1);
        Message message = new Message("body".getBytes(), messageHeader);
        saveDownloadJob(ID, 1, ERROR, UPDATE_COUNT, PROCESSED_ENTRIES);

        idMappingMessageConsumer.onMessage(message);

        await().until(retryCount(ID, MAX_RETRY_COUNT));
        await().atLeast(5, TimeUnit.SECONDS);
        IdMappingDownloadJob job = downloadJobRepository.findById(ID).get();
        assertEquals(ERROR, job.getStatus());
        assertEquals(MAX_RETRY_COUNT, job.getRetried());
        assertEquals(0, job.getUpdateCount());
        assertEquals(0, job.getProcessedEntries());
        assertFalse(asyncDownloadFileHandler.isResultFilePresent(ID));
    }

    private Callable<Boolean> retryCount(String id, int maxRetryCount) {
        return () -> (getRetryCount(id) == maxRetryCount);
    }

    private int getRetryCount(String id) {
        return downloadJobRepository.findById(id).get().getRetried();
    }

    private void createResultFile(String jobId) throws Exception {
        Path idsFile =
                Paths.get(
                        downloadConfigProperties.getResultFilesFolder(),
                        jobId + "." + FileType.GZIP.getExtension());
        BufferedWriter writer =
                Files.newBufferedWriter(
                        idsFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writer.append("");
    }

    private void saveDownloadJob(
            String id,
            int retryCount,
            JobStatus jobStatus,
            long updateCount,
            long processedEntries) {
        idMappingDownloadJobRepository.save(
                IdMappingDownloadJob.builder()
                        .id(id)
                        .status(jobStatus)
                        .updateCount(updateCount)
                        .processedEntries(processedEntries)
                        .retried(retryCount)
                        .build());
        System.out.println();
    }

    private Stream<Arguments> getAllUniProtKBFormats() {
        return UniProtKBIdMappingDownloadRequestValidator.VALID_FORMATS.stream().map(Arguments::of);
    }

    private Stream<Arguments> getAllUniRefFormats() {
        return UniRefIdMappingDownloadRequestValidator.VALID_FORMATS.stream().map(Arguments::of);
    }

    private Stream<Arguments> getAllUniParcFormats() {
        return UniParcIdMappingDownloadRequestValidator.VALID_FORMATS.stream().map(Arguments::of);
    }

    private void validateSupportedRDFMediaTypes(String format, String text) {
        switch (format) {
            case UniProtMediaType.TURTLE_MEDIA_TYPE_VALUE:
                assertTrue(text.contains("@prefix xsd: <http://www.w3.org/2001/XMLSchema#>"));
                assertTrue(text.contains("<SAMPLE> rdf:type up:Protein ;"));
                break;
            case UniProtMediaType.N_TRIPLES_MEDIA_TYPE_VALUE:
                assertTrue(text.contains(SAMPLE_N_TRIPLES));
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
