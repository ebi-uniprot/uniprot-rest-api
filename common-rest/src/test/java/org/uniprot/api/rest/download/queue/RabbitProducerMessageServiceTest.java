package org.uniprot.api.rest.download.queue;

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
import org.uniprot.api.rest.download.file.AsyncDownloadFileHandler;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.job.JobSubmitFeedback;
import org.uniprot.api.rest.request.DownloadRequest;
import org.uniprot.api.rest.request.FakeDownloadRequest;
import org.uniprot.api.rest.request.HashGenerator;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitProducerMessageServiceTest {
    public static final String JOB_ID = "jobId";
    @Mock private MessageConverter converter;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private DownloadJobRepository jobRepository;
    @Mock private HashGenerator<DownloadRequest> hashGenerator;
    @Mock private AsyncDownloadSubmissionRules asyncDownloadSubmissionRules;
    @Mock private AsyncDownloadFileHandler asyncDownloadFileHandler;
    private RabbitProducerMessageService service;
    private DownloadRequest downloadRequest;
    private MessageProperties messageHeader;
    private DownloadJob downloadJob;

    @BeforeEach
    void setUp() {
        service =
                new RabbitProducerMessageService(
                        converter,
                        rabbitTemplate,
                        jobRepository,
                        hashGenerator,
                        asyncDownloadSubmissionRules,
                        asyncDownloadFileHandler);
        downloadRequest = new FakeDownloadRequest();
        messageHeader = new MessageProperties();
        messageHeader.setHeader(JOB_ID, JOB_ID);
        downloadJob = DownloadJob.builder().build();
        downloadJob.setId(JOB_ID);
        downloadJob.setStatus(JobStatus.NEW);
        when(hashGenerator.generateHash(downloadRequest)).thenReturn(JOB_ID);
    }

    @Test
    void sendMessage_whenSubmissionIsAllowed() {
        when(converter.toMessage(any(DownloadRequest.class), any(MessageProperties.class)))
                .thenReturn(new Message(new byte[] {}, messageHeader));
        when(jobRepository.save(any(DownloadJob.class))).thenReturn(downloadJob);
        when(asyncDownloadSubmissionRules.submit(JOB_ID, false))
                .thenReturn(new JobSubmitFeedback(true));

        service.sendMessage(downloadRequest);

        verify(converter, times(1))
                .toMessage(any(DownloadRequest.class), any(MessageProperties.class));
        verify(rabbitTemplate, times(1)).send(any(Message.class));
        verify(jobRepository, times(1)).save(any(DownloadJob.class));
    }

    @Test
    void sendMessage_whenSubmissionIsAllowedWithForce() {
        downloadRequest.setForce(true);
        when(converter.toMessage(any(DownloadRequest.class), any(MessageProperties.class)))
                .thenReturn(new Message(new byte[] {}, messageHeader));
        when(jobRepository.save(any(DownloadJob.class))).thenReturn(downloadJob);
        when(asyncDownloadSubmissionRules.submit(JOB_ID, true))
                .thenReturn(new JobSubmitFeedback(true));

        service.sendMessage(downloadRequest);

        verify(converter, times(1))
                .toMessage(any(DownloadRequest.class), any(MessageProperties.class));
        verify(rabbitTemplate, times(1)).send(any(Message.class));
        verify(jobRepository, times(1)).deleteById(JOB_ID);
        verify(asyncDownloadFileHandler, times(1)).deleteAllFiles(JOB_ID);
        verify(jobRepository, times(1)).save(any(DownloadJob.class));
    }

    @Test
    void sendMessage_whenSubmissionIsNotAllowed() {
        when(asyncDownloadSubmissionRules.submit(JOB_ID, false))
                .thenReturn(new JobSubmitFeedback(false));

        assertThrows(RuntimeException.class, () -> service.sendMessage(downloadRequest));

        verify(converter, times(0)).toMessage(downloadRequest, messageHeader);
        verify(rabbitTemplate, times(0)).send(any());
        verify(jobRepository, times(0)).save(any(DownloadJob.class));
        verify(asyncDownloadFileHandler, never()).deleteAllFiles(any());
    }

    @Test
    void sendMessageWithAmqpException() {
        when(asyncDownloadSubmissionRules.submit(JOB_ID, false))
                .thenReturn(new JobSubmitFeedback(true));
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
}
