package org.uniprot.api.rest.download.queue;

import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import lombok.Data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

@ExtendWith(MockitoExtension.class)
class RabbitProducerMessageServiceTest {
    @Mock private MessageConverter converter;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private DownloadJobRepository jobRepository;
    @Mock private HashGenerator<DownloadRequest> hashGenerator;
    private RabbitProducerMessageService service;
    private DownloadRequest downloadRequest;
    private MessageProperties messageHeader;
    private DownloadJob downloadJob;

    @BeforeEach
    void setUp() {
        service =
                new RabbitProducerMessageService(
                        converter, rabbitTemplate, jobRepository, hashGenerator);
        downloadRequest = new FakeDownloadRequest();
        messageHeader = new MessageProperties();
        messageHeader.setHeader("jobId", "jobId");
        downloadJob = DownloadJob.builder().build();
        downloadJob.setId("jobId");
        downloadJob.setStatus(JobStatus.NEW);
    }

    @Test
    void sendMessage() {
        when(converter.toMessage(any(DownloadRequest.class), any(MessageProperties.class)))
                .thenReturn(new Message(new byte[] {}, messageHeader));
        when(jobRepository.existsById(any())).thenReturn(false);
        when(jobRepository.save(any(DownloadJob.class))).thenReturn(downloadJob);
        service.sendMessage(downloadRequest);
        verify(converter, times(1))
                .toMessage(any(DownloadRequest.class), any(MessageProperties.class));
        verify(rabbitTemplate, times(1)).send(any(Message.class));
        verify(jobRepository, times(1)).save(any(DownloadJob.class));
    }

    @Test
    void sendMessageWithJobExists() {
        when(jobRepository.existsById(any())).thenReturn(true);
        service.sendMessage(downloadRequest);
        verify(converter, times(0)).toMessage(downloadRequest, messageHeader);
        verify(rabbitTemplate, times(0)).send(any());
        verify(jobRepository, times(0)).save(any(DownloadJob.class));
    }

    @Test
    void sendMessageWithAmqpException() {
        when(jobRepository.existsById(any())).thenReturn(false);
        doThrow(AmqpException.class).when(rabbitTemplate).send(any());
        try {
            service.sendMessage(downloadRequest);
            fail("Expected AmqpException to be thrown");
        } catch (AmqpException e) {
            verify(converter, times(1))
                    .toMessage(any(DownloadRequest.class), any(MessageProperties.class));
            verify(rabbitTemplate, times(1)).send(any());
            verify(jobRepository, times(1)).save(any(DownloadJob.class));
            verify(jobRepository, times(1)).deleteById(any());
        }
    }

    @Data
    class FakeDownloadRequest implements DownloadRequest {
        private String query = "dummy";
        private String fields;
        private String sort;
        private String download;
        private String format;
        private boolean isLargeSolrStreamRestricted;
    }
}
