package org.uniprot.api.async.download.messaging.consumer.idmapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static org.uniprot.api.async.download.messaging.consumer.MessageConsumer.CURRENT_RETRIED_COUNT_HEADER;
import static org.uniprot.api.rest.download.model.JobStatus.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.uniprot.api.async.download.AsyncDownloadRestApp;
import org.uniprot.api.async.download.common.AbstractIdMappingIT;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingFileHandler;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {AsyncDownloadRestApp.class})
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@TestPropertySource("classpath:application.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingMessageConsumerIT extends AbstractIdMappingIT {
    private static final String ID = "someId";
    private static final String JOB_ID_HEADER = "jobId";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long UPDATE_COUNT = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long PROCESSED_ENTRIES = 23;

    @Autowired private IdMappingDownloadConfigProperties downloadConfigProperties;

    @Autowired private IdMappingFileHandler asyncDownloadFileHandler;

    @Autowired private IdMappingMessageConsumer idMappingMessageConsumer;

    @Autowired private MessageConverter messageConverter;

    @AfterEach
    public void cleanUp() {
        asyncDownloadFileHandler.deleteAllFiles(ID);
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
        downloadJobRepository.save(
                IdMappingDownloadJob.builder()
                        .id(id)
                        .status(jobStatus)
                        .updateCount(updateCount)
                        .processedEntries(processedEntries)
                        .retried(retryCount)
                        .build());
    }
}
