package org.uniprot.api.idmapping.controller;

import static java.util.function.Predicate.isEqual;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.beans.factory.annotation.Value;
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
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.validator.UniParcIdMappingDownloadRequestValidator;
import org.uniprot.api.idmapping.controller.validator.UniProtKBIdMappingDownloadRequestValidator;
import org.uniprot.api.idmapping.controller.validator.UniRefIdMappingDownloadRequestValidator;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.PredefinedAPIStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;
import org.uniprot.core.uniparc.UniParcEntry;
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

@ActiveProfiles(profiles = {"offline", "asyncDownload"})
@ContextConfiguration(classes = {IdMappingREST.class})
@WebMvcTest(IdMappingDownloadController.class)
@ExtendWith(value = {SpringExtension.class})
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IdMappingDownloadControllerIT {

    @Value("${async.download.queueName}")
    protected String downloadQueue;

    @Value("${async.download.retryQueueName}")
    protected String retryQueue;

    @Value(("${async.download.rejectedQueueName}"))
    protected String rejectedQueue;

    @Value("${download.idFilesFolder}")
    protected String idsFolder;

    @Value("${download.resultFilesFolder}")
    protected String resultFolder;

    @Autowired protected AmqpAdmin amqpAdmin;

    @Autowired protected DownloadJobRepository downloadJobRepository;

    @Autowired protected MockMvc mockMvc;

    @Autowired protected IdMappingJobCacheService cacheService;

    @Autowired private UniProtStoreClient<UniParcEntry> uniParcStoreClient;

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @MockBean(name = "idMappingRdfRestTemplate")
    private RestTemplate idMappingRdfRestTemplate;

    @Autowired private UniProtStoreClient<UniRefEntryLight> uniRefStoreClient;

    private static final UniProtKBEntry TEMPLATE_KB_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    protected static final RabbitMQContainer rabbitMQContainer =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));

    protected static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String JOB_SUBMIT_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/download/run";

    protected static final String JOB_STATUS_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/download/status/{jobId}";

    protected static final String JOB_DETAILS_ENDPOINT =
            IdMappingJobController.IDMAPPING_PATH + "/download/details/{jobId}";

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
        Files.createDirectories(Path.of(this.idsFolder));
        Files.createDirectories(Path.of(this.resultFolder));

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
    public void setUpEach() throws Exception {
        DefaultUriBuilderFactory handler = Mockito.mock(DefaultUriBuilderFactory.class);
        when(idMappingRdfRestTemplate.getUriTemplateHandler()).thenReturn(handler);

        UriBuilder uriBuilder = Mockito.mock(UriBuilder.class);
        when(handler.builder()).thenReturn(uriBuilder);

        URI rdfServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(any(), eq("rdf"), any())).thenReturn(rdfServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(rdfServiceURI), any()))
                .thenReturn(AbstractIdMappingStreamControllerIT.SAMPLE_RDF);

        URI ntServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(any(), eq("nt"), any())).thenReturn(ntServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(ntServiceURI), any()))
                .thenReturn(AbstractIdMappingStreamControllerIT.SAMPLE_N_TRIPLES);

        URI ttlServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(any(), eq("ttl"), any())).thenReturn(ttlServiceURI);
        when(idMappingRdfRestTemplate.getForObject(eq(ttlServiceURI), any()))
                .thenReturn(AbstractIdMappingStreamControllerIT.SAMPLE_TTL);
    }

    private void saveUniProtKB(int i, String isoFormString) {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_KB_ENTRY);
        String acc = String.format("P%05d", i) + isoFormString;
        entryBuilder.primaryAccession(acc);

        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        uniProtKBStoreClient.saveEntry(uniProtKBEntry);
    }

    private void saveUniParc(int i) {
        UniParcEntry uniParcEntry = UniParcEntryMocker.createEntry(i, "UPI0000283A");
        uniParcStoreClient.saveEntry(uniParcEntry);
    }

    private void saveUniRef(UniRefEntry uniRefEntry) {
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(uniRefEntry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        uniRefStoreClient.saveEntry(entryLight);
    }

    @AfterAll
    public void cleanUpData() throws Exception {
        cleanUpFolder(this.idsFolder);
        cleanUpFolder(this.resultFolder);
        downloadJobRepository.deleteAll();
        this.amqpAdmin.purgeQueue(rejectedQueue, true);
        this.amqpAdmin.purgeQueue(downloadQueue, true);
        this.amqpAdmin.purgeQueue(retryQueue, true);
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
        return JobStatus.valueOf(status);
    }

    @Test
    void uniparcDownloadJobDetailsNotFound() throws Exception {
        // Do not save request in idmapping cache
        String jobId = "UNIPARC_JOB_DETAILS_NOT_FOUND";

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
    void uniparcDownloadCanGetJobDetails() throws Exception {
        String jobId = "UNIPARC_JOB_ID_DETAILS";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        cacheIdMappingJob(jobId, "uniparc", JobStatus.FINISHED, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                is("https://localhost/idmapping/download/results/" + jobId)))
                .andExpect(jsonPath("$.fields", is("accession")))
                .andExpect(jsonPath("$.format", is(APPLICATION_JSON_VALUE)));
    }

    @Test
    void uniparcDownloadCanGetJobDetailsWithError() throws Exception {
        String jobId = "UNIPARC_JOB_ID_DETAILS_ERROR";

        cacheIdMappingJob(jobId, "uniparc", JobStatus.FINISHED, List.of());
        DownloadJob downloadJob =
                DownloadJob.builder()
                        .id(jobId)
                        .status(JobStatus.ERROR)
                        .error("Error message")
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
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
    void uniparcDownloadJobSubmittedNotFound() throws Exception {
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
    void uniparcDownloadJobSubmittedBadRequestRequired() throws Exception {
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
    void uniparcDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIPARC_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        cacheIdMappingJob(jobId, "uniparc", JobStatus.RUNNING, mappedIds);

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
    void uniparcDownloadJobSubmittedBadRequestWrongTo() throws Exception {
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
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'uniprotkb', 'uniparc', 'uniref50', 'uniref90' or 'uniref100'.")));
    }

    @Test
    void uniparcDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "uniparc", JobStatus.FINISHED, List.of());

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
                                        "Invalid request received. Invalid download format. Valid values are [text/plain;format=fasta, text/plain;format=tsv, application/json, application/xml, application/rdf+xml, text/turtle, application/n-triples, text/plain;format=list]")));
    }

    @Test
    void uniparcDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIPARC_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "uniparc", JobStatus.FINISHED, List.of());

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
    void uniparcDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIPARC_JOB_SUCCESS";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));
        List<String> unMappedIds = List.of("UPI0000283100", "UPI0000283200");
        cacheIdMappingJob(jobId, "uniparc", JobStatus.FINISHED, mappedIds, unMappedIds);

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
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(this.resultFolder + File.separator + jobId + FileType.GZIP.getExtension());
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
    void uniparcDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UPI0000283A01"));
        mappedIds.add(new IdMappingStringPair("P10002", "UPI0000283A02"));

        String jobId = "UNIPARC_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "uniparc", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().atMost(200, TimeUnit.SECONDS)
                .until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(this.resultFolder + File.separator + jobId + FileType.GZIP.getExtension());
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
    void unirefDownloadJobDetailsNotFound() throws Exception {
        // Do not save request in idmapping cache
        String jobId = "UNIREF_JOB_DETAILS_NOT_FOUND";

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
    void unirefDownloadCanGetJobDetails() throws Exception {
        String jobId = "UNIREF_JOB_ID_DETAILS";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef50_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef50_P03902"));
        cacheIdMappingJob(jobId, "uniref50", JobStatus.FINISHED, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "id,name,types"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                is("https://localhost/idmapping/download/results/" + jobId)))
                .andExpect(jsonPath("$.fields", is("id,name,types")))
                .andExpect(jsonPath("$.format", is(APPLICATION_JSON_VALUE)));
    }

    @Test
    void unirefDownloadCanGetJobDetailsWithError() throws Exception {
        String jobId = "UNIREF_JOB_ID_DETAILS_ERROR";

        cacheIdMappingJob(jobId, "uniref90", JobStatus.FINISHED, List.of());
        DownloadJob downloadJob =
                DownloadJob.builder()
                        .id(jobId)
                        .status(JobStatus.ERROR)
                        .error("Error message")
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
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
    void unirefDownloadJobSubmittedNotFound() throws Exception {
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
        cacheIdMappingJob(jobId, "uniref90", JobStatus.RUNNING, mappedIds);

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
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'uniprotkb', 'uniparc', 'uniref50', 'uniref90' or 'uniref100'.")));
    }

    @Test
    void unirefDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIREF_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "uniref100", JobStatus.FINISHED, List.of());

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

        cacheIdMappingJob(jobId, "uniref100", JobStatus.FINISHED, List.of());

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

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P10001", "UniRef90_P03901"));
        mappedIds.add(new IdMappingStringPair("P10002", "UniRef90_P03902"));
        List<String> unMappedIds = List.of("UniRef90_P03001", "UniRef90_P03002");
        cacheIdMappingJob(jobId, "uniref90", JobStatus.FINISHED, mappedIds, unMappedIds);

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
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(this.resultFolder + File.separator + jobId + FileType.GZIP.getExtension());
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
        cacheIdMappingJob(jobId, "uniref50", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().atMost(200, TimeUnit.SECONDS)
                .until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(this.resultFolder + File.separator + jobId + FileType.GZIP.getExtension());
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
    void uniprotkbDownloadJobDetailsNotFound() throws Exception {
        // Do not save request in idmapping cache
        String jobId = "UNIPROTKB_JOB_DETAILS_NOT_FOUND";

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
    void uniprotkbDownloadCanGetJobDetails() throws Exception {
        String jobId = "UNIPROTKB_JOB_ID_DETAILS";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.FINISHED, mappedIds);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
                                .header(ACCEPT, MediaType.APPLICATION_JSON));
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.redirectURL",
                                is("https://localhost/idmapping/download/results/" + jobId)))
                .andExpect(jsonPath("$.fields", is("accession")))
                .andExpect(jsonPath("$.format", is(APPLICATION_JSON_VALUE)));
    }

    @Test
    void uniprotkbDownloadCanGetJobDetailsWithError() throws Exception {
        String jobId = "UNIPROTKB_JOB_ID_DETAILS_ERROR";

        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.FINISHED, List.of());
        DownloadJob downloadJob =
                DownloadJob.builder()
                        .id(jobId)
                        .status(JobStatus.ERROR)
                        .error("Error message")
                        .build();
        downloadJobRepository.save(downloadJob);

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        response =
                mockMvc.perform(
                        get(JOB_DETAILS_ENDPOINT, jobId)
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
    void uniprotkbDownloadJobSubmittedNotFound() throws Exception {
        // Do not save request in idmapping cache

        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", "JOB_NOT_FOUND")
                                .param("format", "json")
                                .param("fields", "accession"));
        // then
        response.andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(1)))
                .andExpect(jsonPath("$.messages.*", contains("Resource not found")));
    }

    @Test
    void uniprotkbDownloadJobSubmittedBadRequestRequired() throws Exception {
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
    void uniprotkbDownloadJobSubmittedBadRequestNotFinished() throws Exception {
        String jobId = "UNIPROTKB_JOB_RUNNING";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.RUNNING, mappedIds);

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
    void uniprotkbDownloadJobSubmittedBadRequestWrongTo() throws Exception {
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
                                        "Invalid request received. The IdMapping 'to' parameter value is invalid. It should be 'uniprotkb', 'uniparc', 'uniref50', 'uniref90' or 'uniref100'.")));
    }

    @Test
    void uniprotkbDownloadJobSubmittedBadRequestWrongFormat() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_FORMAT";

        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.FINISHED, List.of());

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
    void uniprotkbDownloadJobSubmittedBadRequestInvalidField() throws Exception {
        String jobId = "UNIPROTKB_JOB_WRONG_FIELD";

        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.FINISHED, List.of());

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
    void uniprotkbDownloadJobSubmittedSuccessfully() throws Exception {
        // when
        String jobId = "UNIPROTKB_JOB_SUCCESS";

        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));
        List<String> unMappedIds = List.of("P12345", "P54321");
        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.FINISHED, mappedIds, unMappedIds);

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
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(this.resultFolder + File.separator + jobId + FileType.GZIP.getExtension());
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
                        .map(node -> node.findPath("entryType"))
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
    void uniprotkbDownloadJobSubmittedAllFormats(String format) throws Exception {
        // when
        List<IdMappingStringPair> mappedIds = new ArrayList<>();
        mappedIds.add(new IdMappingStringPair("P00001", "P00001"));
        mappedIds.add(new IdMappingStringPair("P00002", "P00002"));

        String jobId = "UNIPROTKB_JOB_FORMAT_" + cleanFormat(format);
        cacheIdMappingJob(jobId, "uniprotkb", JobStatus.FINISHED, mappedIds);
        ResultActions response =
                mockMvc.perform(
                        post(JOB_SUBMIT_ENDPOINT)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("jobId", jobId)
                                .param("format", format));
        // then
        response.andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.jobId", is(jobId)));

        await().atMost(200, TimeUnit.SECONDS)
                .until(jobProcessed(jobId), isEqual(JobStatus.FINISHED));
        Path resultFilePath =
                Path.of(this.resultFolder + File.separator + jobId + FileType.GZIP.getExtension());
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
        return UniProtKBIdMappingDownloadRequestValidator.validFormat.stream().map(Arguments::of);
    }

    private Stream<Arguments> getAllUniRefFormats() {
        return UniRefIdMappingDownloadRequestValidator.validFormat.stream().map(Arguments::of);
    }

    private Stream<Arguments> getAllUniParcFormats() {
        return UniParcIdMappingDownloadRequestValidator.validFormat.stream().map(Arguments::of);
    }

    private void validateSupportedRDFMediaTypes(String format, String text) {
        switch (format) {
            case UniProtMediaType.TURTLE_MEDIA_TYPE_VALUE:
                assertTrue(text.contains("@prefix xsd: <http://www.w3.org/2001/XMLSchema#>"));
                assertTrue(text.contains("<SAMPLE> rdf:type up:Protein ;"));
                break;
            case UniProtMediaType.N_TRIPLES_MEDIA_TYPE_VALUE:
                assertTrue(text.contains(AbstractIdMappingStreamControllerIT.SAMPLE_N_TRIPLES));
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