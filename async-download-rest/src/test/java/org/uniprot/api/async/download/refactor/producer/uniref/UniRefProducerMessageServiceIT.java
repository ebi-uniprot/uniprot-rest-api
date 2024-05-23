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
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitMQConfig;
import org.uniprot.api.async.download.messaging.listener.uniprotkb.UniProtKBHeartbeatProducer;
import org.uniprot.api.async.download.messaging.listener.uniref.UniRefMessageListener;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefDownloadResultWriter;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.uniref.UniRefContentBasedAndRetriableMessageConsumer;
import org.uniprot.api.async.download.refactor.producer.ProducerMessageServiceIT;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@Import({UniRefProducerMessageServiceIT.UniRefProducerTestConfig.class, UniRefRabbitMQConfig.class})
@EnableConfigurationProperties({UniProtKBDownloadConfigProperties.class})
public class UniRefProducerMessageServiceIT extends ProducerMessageServiceIT {

    @Autowired
    private UniRefProducerMessageService service;

    @Autowired
    UniRefDownloadJobRepository jobRepository;

    @MockBean
    private UniRefContentBasedAndRetriableMessageConsumer uniRefConsumer;

    //TODO: uniRefListener, uniRefWriter and heartBeat need to be remove when we organise HeartBeat bean config
    @MockBean
    private UniRefMessageListener uniRefListener;
    @MockBean
    private UniRefDownloadResultWriter uniRefWriter;
    @MockBean
    private UniProtKBHeartbeatProducer heartBeat;

    @Captor
    ArgumentCaptor<Message> messageCaptor;

    @Test
    void sendMessage_jobAlreadyExistAndNotAllowed() {

    }

    @Test
    void sendMessage_withoutForceAndAllowed() {

    }

    @Test
    void sendMessage_withForceAndAllowed() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("query value");
        request.setSort("accession asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);
        assertEquals("60ba2e259320dcb5a23f2e432c8f6bc6d8ed417f", jobId);

        Mockito.verify(uniRefConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
    }

    @Test
    void sendMessage_withForceAndAllowed2() {
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
    void sendMessage_withForceAndAllowed3() {
        UniRefDownloadRequest request = new UniRefDownloadRequest();
        request.setQuery("query3 value");
        request.setSort("accession3 asc");
        request.setFormat("json");
        request.setFields("accession,gene");

        String jobId = service.sendMessage(request);

        assertEquals("975516c6b26115d2d1e0e3a0903feaae618e0bfb", jobId);
        Mockito.verify(uniRefConsumer, Mockito.timeout(1000).times(1)).onMessage(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        validateMessage(message, jobId, request);

        //Validate cached data
        DownloadJob downloadJob = jobRepository.findById(jobId)
                .orElseThrow(AssertionFailedError::new);
        validateDownloadJob(jobId, downloadJob, request);
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
