package org.uniprot.api.async.download.refactor.producer.uniprotkb;

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
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitMQConfig;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBMessageListener;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBDownloadResultWriter;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.uniprotkb.UniProtKBContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.producer.ProducerMessageServiceIT;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.IllegalDownloadJobSubmissionException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({UniProtKBProducerMessageServiceIT.UniProtKBProducerTestConfig.class, UniProtKBRabbitMQConfig.class})
@EnableConfigurationProperties({UniProtKBDownloadConfigProperties.class})
class UniProtKBProducerMessageServiceIT extends ProducerMessageServiceIT {

    @Autowired
    private UniProtKBProducerMessageService service;

    @Autowired
    UniProtKBDownloadJobRepository jobRepository;

    @Autowired
    UniProtKBAsyncDownloadFileHandler fileHandler;

    @Autowired
    UniProtKBDownloadConfigProperties uniProtKBDownloadConfigProperties;

    @MockBean
    private UniProtKBContentBasedAndRetriableMessageConsumer uniProtKBConsumer;

    //TODO: uniProtKBListener and uniProtKBWriter need to be remove when we organise HeartBeat bean config
    @MockBean
    private UniProtKBMessageListener uniProtKBListener;
    @MockBean
    private UniProtKBDownloadResultWriter uniProtKBWriter;

    @Captor
    ArgumentCaptor<Message> messageCaptor;

    @Test
    void sendMessage_withSuccess() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query2 value");
        request.setSort("accession2 asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);

        assertEquals("1a9848044d4910bc55dccdaa673f4713d5ab091e", jobId);
        Mockito.verify(uniProtKBConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withSuccessForceAndIdleJobAllowedAndCleanResources() throws Exception{
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");
        request.setForce(true);
        String jobId = "60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f";

        // Reproduce Idle Job in Running Status in and files created
        createJobFiles(jobId, fileHandler, uniProtKBDownloadConfigProperties);
        LocalDateTime idleSince = LocalDateTime.now().minusMinutes(20);
        UniProtKBDownloadJob idleJob = new UniProtKBDownloadJob(jobId, JobStatus.RUNNING, null, idleSince, null, 0,request.getQuery(), request.getFields(), request.getSort(),null, request.getFormat(), 100, 10, 1);
        jobRepository.save(idleJob);

        String jobIdResult = service.sendMessage(request);
        assertEquals(jobIdResult, jobId);

        //Validate message received in Listener
        Mockito.verify(uniProtKBConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
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
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("AlreadyExist");
        request.setFormat("json");
        String jobId = "a63c4f8dd0687bf13338a98e7115984bf3e1b52d";
        UniProtKBDownloadJob runningJob = new UniProtKBDownloadJob(jobId, JobStatus.RUNNING, LocalDateTime.now(), LocalDateTime.now(), null, 0,request.getQuery(), request.getFields(), request.getSort(), null, request.getFormat(), 0, 0, 0);
        jobRepository.save(runningJob);

        IllegalDownloadJobSubmissionException submitionError = assertThrows(IllegalDownloadJobSubmissionException.class, () -> service.sendMessage(request));
        assertEquals("Job with id "+ jobId +" has already been submitted", submitionError.getMessage());
    }

    @Test
    void sendMessage_WithoutFormatDefaultToJson() {
        UniProtKBDownloadRequest request = new UniProtKBDownloadRequest();
        request.setQuery("Not using format");

        String jobId = "712dc7afcd2514a178e887d68400421666cde5ed";
        String resultJobId = service.sendMessage(request);
        assertEquals(jobId, resultJobId);
        request.setFormat("json");

        Mockito.verify(uniProtKBConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);
    }

    @TestConfiguration
    @ComponentScan({"org.uniprot.api.async.download.refactor.producer.uniprotkb",
            "org.uniprot.api.async.download.refactor.messaging.uniprotkb",
            "org.uniprot.api.async.download.refactor.service.uniprotkb",
            "org.uniprot.api.async.download.refactor.consumer.uniprotkb",
            "org.uniprot.api.async.download.messaging.config.uniprotkb",
            "org.uniprot.api.async.download.messaging.result.uniprotkb",
            "org.uniprot.api.async.download.messaging.listener.common",
            "org.uniprot.api.async.download.messaging.listener.uniprotkb",
            "org.uniprot.api.async.download.messaging.producer.uniprotkb"})
    static class UniProtKBProducerTestConfig {
    }
}
