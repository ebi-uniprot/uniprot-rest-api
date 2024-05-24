package org.uniprot.api.async.download.refactor.producer.uniref;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitMQConfig;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.uniref.UniRefContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.producer.ProducerMessageServiceIT;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({UniRefProducerMessageServiceIT.UniRefProducerTestConfig.class, UniRefRabbitMQConfig.class})
@EnableConfigurationProperties({UniRefDownloadConfigProperties.class})
public class UniRefProducerMessageServiceIT extends ProducerMessageServiceIT {

    @Autowired
    private UniRefProducerMessageService service;

    @Autowired
    UniRefDownloadJobRepository jobRepository;

    @Autowired
    UniRefAsyncDownloadFileHandler fileHandler;

    @Autowired
    UniRefDownloadConfigProperties uniRefDownloadConfigProperties;

    @MockBean
    private UniRefContentBasedAndRetriableMessageConsumer uniRefConsumer;
    @MockBean
    private UniProtKBHeartbeatProducer heartBeat;

    @Captor
    ArgumentCaptor<Message> messageCaptor;


    @Test
    void sendMessage_withSuccess() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);

        assertEquals("1a9848044d4910bc55dccdaa673f4713d5ab091e", jobId);
        Mockito.verify(uniRefConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withSuccessForceAndIdleJobAllowedAndCleanResources() throws Exception{
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        String jobId = "60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f";

        // Reproduce Idle Job in Running Status in and files created
        createJobFiles(jobId, fileHandler, uniRefDownloadConfigProperties);
        LocalDateTime idleSince = LocalDateTime.now().minusMinutes(20);
        UniRefDownloadJob idleJob = new UniRefDownloadJob(jobId, JobStatus.RUNNING, null, idleSince, null, 0,request.getQuery(), request.getFields(), request.getSort(),null, request.getFormat(), 100, 10, 1);
        jobRepository.save(idleJob);

        String jobIdResult = service.sendMessage(request);
        assertEquals(jobIdResult, jobId);

        //Validate message received in Listener
        Mockito.verify(uniRefConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data is a new Job
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);

        //Validate idle job files were deleted
        assertFalse(fileHandler.isIdFileExist(jobId));
        assertFalse(fileHandler.isResultFileExist(jobId));
    }

    @Test
    void sendMessage_jobAlreadyRunningAndNotAllowed() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        String jobId = "a63c4f8dd0687bf13338a98e7115984bf3e1b52d";
        UniRefDownloadJob runningJob = new UniRefDownloadJob(jobId, JobStatus.RUNNING, LocalDateTime.now(), LocalDateTime.now(), null, 0,request.getQuery(), request.getFields(), request.getSort(), null, request.getFormat(), 100, 20, 2);
        jobRepository.save(runningJob);

        IllegalDownloadJobSubmissionException submitionError = assertThrows(IllegalDownloadJobSubmissionException.class, () -> service.sendMessage(request));
        assertEquals("Job with id "+ jobId +" has already been submitted", submitionError.getMessage());
    }

    @Test
    void sendMessage_WithoutFormatDefaultToJson() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("Not using format");

        String jobId = "712dc7afcd2514a178e887d68400421666cde5ed";
        String resultJobId = service.sendMessage(request);
        assertEquals(jobId, resultJobId);
        request.setFormat("json");

        Mockito.verify(uniRefConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);
    }


    @TestConfiguration
    @ComponentScan({"org.uniprot.api.async.download.refactor.producer.uniref",
            "org.uniprot.api.async.download.refactor.messaging.uniref",
            "org.uniprot.api.async.download.refactor.service.uniref",
            "org.uniprot.api.async.download.refactor.consumer.uniref",
            "org.uniprot.api.async.download.messaging.config.uniref",
            "org.uniprot.api.async.download.messaging.result.uniref",
            "org.uniprot.api.async.download.messaging.listener.common",
            "org.uniprot.api.async.download.messaging.listener.uniref",
            "org.uniprot.api.async.download.messaging.producer.uniref"})
    static class UniRefProducerTestConfig {
    }
}
