package org.uniprot.api.async.download.messaging.producer.idmapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.rest.output.UniProtMediaType.valueOf;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitMQConfig;
import org.uniprot.api.async.download.messaging.consumer.idmapping.IdMappingMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.BasicProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.job.idmapping.IdMappingDownloadJob;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.impl.RedisCacheMappingJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;

import junit.framework.AssertionFailedError;

@ActiveProfiles(profiles = {"producerIT"})
@ExtendWith(SpringExtension.class)
@Import({
    IdMappingProducerMessageServiceIT.IdMappingProducerTestConfig.class,
    IdMappingRabbitMQConfig.class,
    RedisConfigTest.class
})
class IdMappingProducerMessageServiceIT extends BasicProducerMessageServiceIT {

    private static final String ID_MAPPING_JOB_ID_VALUE = "idMappingJobIdValue";

    @Autowired private IdMappingProducerMessageService service;

    @Autowired private IdMappingJobCacheService idMappingJobCacheService;

    @Autowired private IdMappingDownloadJobRepository jobRepository;

    @Autowired private IdMappingFileHandler fileHandler;

    @Autowired private IdMappingDownloadConfigProperties idMappingDownloadConfigProperties;

    @MockBean private IdMappingMessageConsumer idMappingMessageConsumer;

    @Captor ArgumentCaptor<Message> messageCaptor;

    @Test
    void sendMessage_withSuccess() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setIdMappingJobId(ID_MAPPING_JOB_ID_VALUE);
        request.setFormat("fasta");
        request.setFields("accession,gene");
        createIDMappingJob(JobStatus.FINISHED);

        String jobId = service.sendMessage(request);

        assertEquals("69f040f9379647d61adb703c357466fef6f07804", jobId);
        Mockito.verify(idMappingMessageConsumer, Mockito.timeout(1000).times(1))
                .onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        // Validate cached data
        DownloadJob downloadJob =
                jobRepository.findById(jobId).orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withSuccessForceAndIdleJobAllowedAndCleanResources() throws Exception {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setIdMappingJobId(ID_MAPPING_JOB_ID_VALUE);
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        String jobId = "054de54c6e8ef8697b7863cfedb5afac75e156b0";
        createIDMappingJob(JobStatus.FINISHED);
        // Reproduce Idle Job in Running Status in and files created
        createJobFiles(jobId);
        // Validate idle job files were deleted
        LocalDateTime idleSince = LocalDateTime.now().minusMinutes(20);
        IdMappingDownloadJob idleJob =
                new IdMappingDownloadJob(
                        jobId,
                        JobStatus.RUNNING,
                        null,
                        idleSince,
                        null,
                        0,
                        null,
                        null,
                        null,
                        null,
                        request.getFormat(),
                        100,
                        10,
                        1);
        jobRepository.save(idleJob);

        String jobIdResult = service.sendMessage(request);
        assertEquals(jobIdResult, jobId);

        // Validate message received in Listener
        Mockito.verify(idMappingMessageConsumer, Mockito.timeout(1000).times(1))
                .onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        // Validate cached data is a new Job
        DownloadJob downloadJob =
                jobRepository.findById(jobId).orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);

        // Validate idle job files were deleted
        assertFalse(fileHandler.isIdFilePresent(jobId));
        assertFalse(fileHandler.isResultFilePresent(jobId));
    }

    @Test
    void sendMessage_jobAlreadyRunningAndNotAllowed() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setIdMappingJobId(ID_MAPPING_JOB_ID_VALUE);
        request.setFormat("json");
        String jobId = "6ce47574dc980c6c4d49e62483eaee999fb51f60";
        createIDMappingJob(JobStatus.FINISHED);
        IdMappingDownloadJob runningJob =
                new IdMappingDownloadJob(
                        jobId,
                        JobStatus.RUNNING,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        null,
                        0,
                        null,
                        request.getFields(),
                        null,
                        null,
                        request.getFormat(),
                        0,
                        0,
                        0);
        jobRepository.save(runningJob);

        IllegalDownloadJobSubmissionException submitionError =
                assertThrows(
                        IllegalDownloadJobSubmissionException.class,
                        () -> service.sendMessage(request));
        assertEquals(
                "Job with id " + jobId + " has already been submitted",
                submitionError.getMessage());
    }

    @Test
    void sendMessage_IdMappingNotFinishedNotAllowed() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setIdMappingJobId(ID_MAPPING_JOB_ID_VALUE);
        request.setFormat("json");

        createIDMappingJob(JobStatus.RUNNING);

        IllegalDownloadJobSubmissionException submitionError =
                assertThrows(
                        IllegalDownloadJobSubmissionException.class,
                        () -> service.sendMessage(request));
        assertEquals(
                "ID Mapping Job id " + ID_MAPPING_JOB_ID_VALUE + " not yet finished",
                submitionError.getMessage());
    }

    @Test
    void sendMessage_IdMappingNotFoundNotAllowed() {
        String idMappingNotValid = "INVALID";
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setIdMappingJobId(idMappingNotValid);
        request.setFormat("json");

        IllegalDownloadJobSubmissionException submitionError =
                assertThrows(
                        IllegalDownloadJobSubmissionException.class,
                        () -> service.sendMessage(request));
        assertEquals(
                "ID Mapping Job id " + idMappingNotValid + " not found",
                submitionError.getMessage());
    }

    @Test
    void sendMessage_WithoutFormatDefaultToJson() {
        IdMappingDownloadRequest request = new IdMappingDownloadRequest();
        request.setIdMappingJobId(ID_MAPPING_JOB_ID_VALUE);
        createIDMappingJob(JobStatus.FINISHED);

        String jobId = "6ce47574dc980c6c4d49e62483eaee999fb51f60";
        String resultJobId = service.sendMessage(request);
        assertEquals(jobId, resultJobId);
        request.setFormat("json");

        Mockito.verify(idMappingMessageConsumer, Mockito.timeout(1000).times(1))
                .onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);
    }

    private void validateDownloadJob(
            String jobId, DownloadJob downloadJob, IdMappingDownloadRequest request) {
        validateDownloadJob(jobId, downloadJob);
        assertNull(downloadJob.getQuery());
        assertNull(downloadJob.getSort());
        assertEquals(request.getFields(), downloadJob.getFields());
        assertEquals(valueOf(request.getFormat()), valueOf(downloadJob.getFormat()));
    }

    private void validateMessage(Message message, String jobId, IdMappingDownloadRequest request) {
        validateMessage(message, jobId);
        IdMappingDownloadRequest submittedRequest =
                (IdMappingDownloadRequest) converter.fromMessage(message);
        assertEquals(request, submittedRequest);
    }

    private void createIDMappingJob(JobStatus jobStatus) {
        idMappingJobCacheService.put(
                ID_MAPPING_JOB_ID_VALUE,
                IdMappingJob.builder()
                        .jobId(ID_MAPPING_JOB_ID_VALUE)
                        .jobStatus(jobStatus)
                        .idMappingResult(IdMappingResult.builder().build())
                        .build());
    }

    @Override
    protected DownloadConfigProperties getDownloadConfigProperties() {
        return idMappingDownloadConfigProperties;
    }

    @Override
    protected FileHandler getMapFileHandler() {
        return fileHandler;
    }

    @TestConfiguration
    @ComponentScan({
        "org.uniprot.api.async.download.messaging.producer.idmapping",
        "org.uniprot.api.async.download.mq.idmapping",
        "org.uniprot.api.async.download.service.idmapping",
        "org.uniprot.api.async.download.messaging.config.idmapping",
        "org.uniprot.api.async.download.messaging.result.idmapping",
        "org.uniprot.api.async.download.messaging.consumer.heartbeat.idmapping",
        "org.uniprot.api.async.download.messaging.producer.idmapping"
    })
    static class IdMappingProducerTestConfig {
        @Profile("producerIT")
        @Bean
        public IdMappingJobCacheService idMappingJobCacheService(RedissonClient redissonClient) {
            Map<String, CacheConfig> config = new HashMap<>();
            config.put("testMap", null);
            CacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
            Cache mappingCache = cacheManager.getCache("testMap");
            return new RedisCacheMappingJobService(mappingCache);
        }
    }
}
