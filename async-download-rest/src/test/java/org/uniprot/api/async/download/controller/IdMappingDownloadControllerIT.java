package org.uniprot.api.async.download.controller;

import static java.util.function.Predicate.isEqual;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.async.download.common.RedisUtil.jobCreatedInRedis;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.createEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import org.uniprot.api.async.download.controller.validator.UniParcIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.controller.validator.UniProtKBIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.controller.validator.UniRefIdMappingDownloadRequestValidator;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingJobRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.IdMappingJobService;
import org.uniprot.api.idmapping.common.service.TestConfig;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.jayway.jsonpath.JsonPath;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {TestConfig.class, AsyncDownloadRestApp.class})
@WebMvcTest(IdMappingDownloadController.class)
@ExtendWith(value = {SpringExtension.class})
@AutoConfigureWebClient
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdMappingDownloadControllerIT {

    @Autowired private IdMappingAsyncConfig idMappingAsyncConfig;

    @Autowired protected AmqpAdmin amqpAdmin;

    @Autowired protected IdMappingDownloadJobRepository downloadJobRepository;

    @Autowired protected MockMvc mockMvc;

    @Autowired protected IdMappingJobCacheService cacheService;

    @Autowired private UniProtStoreClient<UniParcEntryLight> uniParcLightStoreClient;

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Autowired private UniParcCrossReferenceStoreClient uniParcCrossReferenceStoreClient;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate idMappingRdfRestTemplate;

    @Qualifier("uniRefLightStoreClient")
    @Autowired
    private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    private static final UniProtKBEntry TEMPLATE_KB_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    @Container
    protected static final RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    @Container
    protected static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String JOB_SUBMIT_ENDPOINT =
            IdMappingJobService.IDMAPPING_PATH + "/download/run";

    protected static final String JOB_STATUS_ENDPOINT =
            IdMappingJobService.IDMAPPING_PATH + "/download/status/{jobId}";

    protected static final String JOB_DETAILS_ENDPOINT =
            IdMappingJobService.IDMAPPING_PATH + "/download/details/{jobId}";

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
    public void cleanUpData() {
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
                        .build();
        cacheService.put(jobId, idmappingJob);
    }

    protected Callable<JobStatus> jobProcessed(String jobId) {
        return () -> getJobStatus(jobId);
    }

    protected JobStatus getJobStatus(String jobId) throws Exception {

        await().until(jobCreatedInRedis(downloadJobRepository, jobId));

        MockHttpServletRequestBuilder requestBuilder =
                get(JOB_STATUS_ENDPOINT, jobId).header(ACCEPT, MediaType.APPLICATION_JSON);
        ResultActions response = this.mockMvc.perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobStatus", notNullValue()));
        String responseAsString = response.andReturn().getResponse().getContentAsString();
        assertNotNull(responseAsString, "status response should not be null");
        String status = MAPPER.readTree(responseAsString).get("jobStatus").asText();
        assertNotNull(status, "status should not be null");
        JobStatus jobStatus = JobStatus.valueOf(status);
        long totalEntries = MAPPER.readTree(responseAsString).get("totalEntries").asLong();
        long processedEntries = MAPPER.readTree(responseAsString).get("processedEntries").asLong();
        if (JobStatus.FINISHED.equals(jobStatus)) {
            assertEquals(totalEntries, processedEntries);
        } else {
            assertTrue(processedEntries <= totalEntries);
        }
        return jobStatus;
    }

    @Test
    void downloadJobDetailsNotFound() throws Exception {
        // Do not save request in idmapping cache
        String jobId = "JOB_DETAILS_NOT_FOUND";

        ResultActions response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void downloadCanGetJobDetails() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS";
        String asynchJobId = "26us1hCFLb";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asynchJobId)));

        await().until(jobProcessed(asynchJobId), isEqual(JobStatus.FINISHED));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, asynchJobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                is("https://localhost/idmapping/download/results/" + asynchJobId)))
                .andExpect(jsonPath("$.fields", is("accession")))
                .andExpect(jsonPath("$.format", is(APPLICATION_JSON_VALUE)));
    }

    @Test
    void downloadCanGetJobDetailsWithError() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder()
                        .id(asyncJobId)
                        .status(JobStatus.ERROR)
                        .error("Error message")
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, asyncJobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.errors[0].code", is(PredefinedAPIStatus.SERVER_ERROR.getCode())))
                .andExpect(jsonPath("$.errors[0].message", is(downloadJob.getError())));
    }

    @Test
    void resubmit_withForceOnAlreadyFinishedJob() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder().id(asyncJobId).status(JobStatus.FINISHED).build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("has already been finished")));
    }

    @Test
    void resubmit_withForceOnAlreadyFinishedJobWithAcceptAll() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder().id(asyncJobId).status(JobStatus.FINISHED).build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.ALL)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("has already been finished")));
    }

    @Test
    void resubmit_withForceOnAlreadyFinishedJobWithoutAccept() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder().id(asyncJobId).status(JobStatus.FINISHED).build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("has already been finished")));
    }

    @Test
    void resubmit_withForceOnAlreadyFailedJobAfterMaxRetry() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_RETRY";
        String asyncJobId = "3Q4ID5wQxK";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        List<String> unMappedIds = List.of("UPI0000283100", "UPI0000283200");

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, mappedIds, unMappedIds);
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder()
                        .id(asyncJobId)
                        .retried(3)
                        .status(JobStatus.ERROR)
                        .build();
        downloadJobRepository.save(downloadJob);

        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());
        Assertions.assertTrue(resultFilePath.toFile().createNewFile());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message").doesNotExist());

        validateSuccessIdMappingResult(asyncJobId, resultFilePath, unMappedIds);
    }

    @Test
    void resubmit_withForceOnAlreadyFailedJobBeforeMaxRetry() throws Exception {
        String idMappingJobId = "JOB_ID_DETAILS_ERROR";
        String asyncJobId = "jzDe7R1age";

        cacheIdMappingJob(idMappingJobId, "UniParc", JobStatus.FINISHED, List.of());
        IdMappingDownloadJob downloadJob =
                IdMappingDownloadJob.builder()
                        .id(asyncJobId)
                        .retried(1)
                        .status(JobStatus.ERROR)
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", idMappingJobId)
                                .param("format", "json")
                                .param("fields", "accession")
                                .param("force", "true"));

        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)))
                .andExpect(jsonPath("$.message", containsString("already being retried")));
    }

    @Test
    void downloadJobSubmittedNotFound() throws Exception {
        // Do not save request in idmapping cache

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", "JOB_NOT_FOUND")
                                .param("format", "json"));
        // then
        response.andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestRequired() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'jobId' is a required parameter",
                                        "'format' is a required parameter")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIPARC_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        cacheIdMappingJob(jobId, "UniParc", JobStatus.RUNNING, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. IdMapping Job must be finished, before we start to download it.")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestWrongTo() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_TO";

        cacheIdMappingJob(jobId, "invalid", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniProtKB/Swiss-Prot', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=list]")));
    }

    @Test
    void uniParcDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "fasta")
                                .param("fields", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid UniParc fields parameter value: [invalid].")));
    }

    @Test
    void uniParcDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIPARC_JOB_SUCCESS";
        String asyncJobId = "omGWzNTCvM";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        List<String> unMappedIds = List.of("UPI0000283100", "UPI0000283200");
        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, mappedIds, unMappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "upi,organism"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
                                + FileType.GZIP.getExtension());

        validateSuccessIdMappingResult(asyncJobId, resultFilePath, unMappedIds);
    }

    @ParameterizedTest(name = "[{index}] with format {0}")
    @MethodSource("getAllUniParcFormats")
    void uniParcDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));

        String jobId = "UNIPARC_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "UniParc", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String jobResponse = response.andReturn().getResponse().getContentAsString();
        String asyncJobId = JsonPath.read(jobResponse, "$.jobId");

        await().atMost(200, TimeUnit.SECONDS)
                .until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
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

    @Test
    void unirefDownloadJobSubmittedBadRequestRequired() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'jobId' is a required parameter",
                                        "'format' is a required parameter")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIREF_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef90_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef90_P03902"));
        cacheIdMappingJob(jobId, "UniRef90", JobStatus.RUNNING, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. IdMapping Job must be finished, before we start to download it.")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestWrongTo() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_TO";

        cacheIdMappingJob(jobId, "invalid", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniProtKB/Swiss-Prot', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "UniRef100", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, text/plain;format=list, application/rdf+xml, text/turtle, application/n-triples]")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "UniRef100", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "fasta")
                                .param("fields", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid UniRef fields parameter value: [invalid].")));
    }

    @Test
    void unirefDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIREF_JOB_SUCCESS";
        String asyncJobId = "9IwZyXSe7G";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef90_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef90_P03902"));
        List<String> unMappedIds = List.of("UniRef90_P03001", "UniRef90_P03002");
        cacheIdMappingJob(jobId, "UniRef90", JobStatus.FINISHED, mappedIds, unMappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "id,name,common_taxon"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
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
    void unirefDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef50_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef50_P03902"));

        String jobId = "UNIREF_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "UniRef50", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String jobResponse = response.andReturn().getResponse().getContentAsString();
        String asyncJobId = JsonPath.read(jobResponse, "$.jobId");

        await().atMost(200, TimeUnit.SECONDS)
                .until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
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
    void uniProtKBDownloadJobSubmittedBadRequestRequired() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT).header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'jobId' is a required parameter",
                                        "'format' is a required parameter")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIPROTKB_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.RUNNING, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. IdMapping Job must be finished, before we start to download it.")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestWrongTo() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_TO";

        cacheIdMappingJob(jobId, "invalid", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'UniProtKB', 'UniProtKB/Swiss-Prot', 'UniParc', 'UniRef50', 'UniRef90' or 'UniRef100'.")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=flatfile, text/plain;format=gff, text/plain;format=list]")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, List.of());

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "fasta")
                                .param("fields", "invalid"));

        // then
        response.andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "Invalid request received. Invalid UniProtKB fields parameter value: [invalid].")));
    }

    @Test
    void uniProtKBDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIPROTKB_JOB_SUCCESS";
        String asyncJobId = "dMcurbvIQ8";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        List<String> unMappedIds = List.of("P12345", "P54321");
        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, mappedIds, unMappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "accession,gene_names,organism_name"));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(asyncJobId)));

        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
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
    void uniProtKBDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));

        String jobId = "UNIPROTKB_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "UniProtKB", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        String jobResponse = response.andReturn().getResponse().getContentAsString();
        String asyncJobId = JsonPath.read(jobResponse, "$.jobId");

        await().atMost(200, TimeUnit.SECONDS)
                .until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(
                        idMappingAsyncConfig.getResultFolder()
                                + File.separator
                                + asyncJobId
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

    private void validateSuccessIdMappingResult(
            String asyncJobId, Path resultFilePath, List<String> unMappedIds) throws IOException {
        await().until(jobProcessed(asyncJobId), isEqual(JobStatus.FINISHED));
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
}
