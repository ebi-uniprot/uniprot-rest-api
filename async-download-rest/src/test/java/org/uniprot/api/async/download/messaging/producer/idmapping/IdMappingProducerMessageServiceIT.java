package org.uniprot.api.async.download.messaging.producer.idmapping;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.common.RedisConfigTest;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingRabbitMQConfig;
import org.uniprot.api.async.download.messaging.consumer.idmapping.IdMappingContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.messaging.producer.BasicProducerMessageServiceIT;
import org.uniprot.api.async.download.messaging.repository.IdMappingDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.idmapping.IdMappingAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.async.download.service.idmapping.IdMappingJobService;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.idmapping.common.service.config.IdMappingConfig;

@ExtendWith(SpringExtension.class)
@Import({
    IdMappingProducerMessageServiceIT.IdMappingProducerTestConfig.class,
    IdMappingRabbitMQConfig.class,
    RedisConfigTest.class,
    IdMappingConfig.class
})
class IdMappingProducerMessageServiceIT extends BasicProducerMessageServiceIT {

    @Autowired private IdMappingProducerMessageService service;

    @Autowired private IdMappingJobService idMappingJobService;

    @Autowired private IdMappingJobCacheService idMappingJobCacheService;

    @Autowired private IdMappingAsyncDownloadSubmissionRules idMappingAsyncDownloadSubmissionRules;

    @Autowired private IdMappingDownloadJobRepository jobRepository;

    @Autowired private IdMappingAsyncDownloadFileHandler fileHandler;

    @Autowired private IdMappingDownloadConfigProperties idMappingDownloadConfigProperties;

    @MockBean private IdMappingContentBasedAndRetriableMessageConsumer idMappingMessageConsumer;

    @Captor ArgumentCaptor<Message> messageCaptor;

    @Test
    void toRemove() {
        assertNotNull(service);
        assertNotNull(jobRepository);
        assertNotNull(fileHandler);
        assertNotNull(idMappingDownloadConfigProperties);
    }
    // TODO: We need to change the comment above in order to enable these tests.
    /*
        @Test
        void sendMessage_withSuccess() {
            IdMappingDownloadRequest request = new IdMappingDownloadRequest();
            request.setJobId("query2 value");
            request.setFormat("json");
            request.setFields("accession,gene");

            String jobId = service.sendMessage(request);

            assertEquals("0ab94b810082083eb8f235574ca21345893bb194", jobId);
            Mockito.verify(uniprotkbIdMappingConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
            Message message = messageCaptor.getValue();
            //validateMessage(message, jobId, request);

            //Validate cached data
            DownloadJob downloadJob = jobRepository.findById(jobId)
                    .orElseThrow(AssertionFailedError::new);
            //validateDownloadJob(jobId, downloadJob, request);
        }

        @Test
        void sendMessage_withSuccessForceAndIdleJobAllowedAndCleanResources() throws Exception{
            IdMappingDownloadRequest request = new IdMappingDownloadRequest();
            request.setJobId("query value");
            request.setFormat("json");
            request.setFields("accession,gene");
            request.setForce(true);
            String jobId = "60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f";

            // Reproduce Idle Job in Running Status in and files created
            createJobFiles(jobId, fileHandler, idMappingDownloadConfigProperties);
            LocalDateTime idleSince = LocalDateTime.now().minusMinutes(20);
            IdMappingDownloadJob idleJob = new IdMappingDownloadJob(jobId, JobStatus.RUNNING, null, idleSince, null, 0,null, null, null,null, request.getFormat(), 100, 10, 1);
            jobRepository.save(idleJob);

            String jobIdResult = service.sendMessage(request);
            assertEquals(jobIdResult, jobId);

            //Validate message received in Listener
            Mockito.verify(uniprotkbIdMappingConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
            Message message = messageCaptor.getValue();
            //validateMessage(message, jobId, request);

            //Validate cached data is a new Job
            DownloadJob downloadJob = jobRepository.findById(jobId)
                    .orElseThrow(AssertionFailedError::new);
            //validateDownloadJob(jobId, downloadJob, request);

            //Validate idle job files were deleted
            assertFalse(fileHandler.isIdFileExist(jobId));
            assertFalse(fileHandler.isResultFileExist(jobId));
        }

        @Test
        void sendMessage_jobAlreadyRunningAndNotAllowed() {
            IdMappingDownloadRequest request = new IdMappingDownloadRequest();
            request.setJobId("AlreadyExist");
            request.setFormat("json");
            String jobId = "a63c4f8dd0687bf13338a98e7115984bf3e1b52d";
            IdMappingDownloadJob runningJob = new IdMappingDownloadJob(jobId, JobStatus.RUNNING, LocalDateTime.now(), LocalDateTime.now(), null, 0,null, request.getFields(), null, null, request.getFormat(), 0, 0, 0);
            jobRepository.save(runningJob);

            IllegalDownloadJobSubmissionException submitionError = assertThrows(IllegalDownloadJobSubmissionException.class, () -> service.sendMessage(request));
            assertEquals("Job with id "+ jobId +" has already been submitted", submitionError.getMessage());
        }

        @Test
        void sendMessage_WithoutFormatDefaultToJson() {
            IdMappingDownloadRequest request = new IdMappingDownloadRequest();
            request.setJobId("Not using format");

            String jobId = "82efe9a0b5c831797202f2e5a40fe33f9e38eda9";
            String resultJobId = service.sendMessage(request);
            assertEquals(jobId, resultJobId);
            request.setFormat("json");

            Mockito.verify(uniprotkbIdMappingConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
            Message message = messageCaptor.getValue();
            //validateMessage(message, jobId, request);
        }
    */
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
    static class IdMappingProducerTestConfig {}
}
