package org.uniprot.api.async.download.queue.idmapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.uniprot.api.async.download.queue.common.DownloadConfigProperties;
import org.uniprot.api.async.download.repository.DownloadJobRepository;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.request.IdMappingDownloadRequestImpl;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

@ExtendWith({MockitoExtension.class})
class IdMappingMessageListenerIT {

    @Mock private MessageConverter converter;

    @Mock DownloadConfigProperties downloadConfigProperties;

    @Mock IdMappingAsyncDownloadQueueConfigProperties asyncDownloadQueueConfigProperties;

    @Mock DownloadJobRepository jobRepository;

    @Mock private RabbitTemplate rabbitTemplate;

    @Mock IdMappingDownloadResultWriterFactory writerFactory;

    @Mock private IdMappingJobCacheService idMappingJobCacheService;

    @Mock UniProtKBIdMappingDownloadResultWriter uniProtKBIdMappingDownloadResultWriter;

    @InjectMocks private IdMappingMessageListener idMappingMessageListener;

    @Test
    void testOnMessageFinishedWithSuccess() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = getIdMappingDownloadRequest(jobId);
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        // stub
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedId(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .mappedId(IdMappingStringPair.builder().from("P05067").to("P05067").build())
                        .build();
        String to = "UniProtKB";
        IdMappingJob idMappingJob = getIdMappingJob(to);
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);
        Mockito.verify(this.uniProtKBIdMappingDownloadResultWriter, atMostOnce())
                .writeResult(
                        downloadRequest, idMappingResult, downloadJob, MediaType.APPLICATION_JSON);
        when(this.writerFactory.getResultWriter(to))
                .thenReturn(
                        (AbstractIdMappingDownloadResultWriter)
                                this.uniProtKBIdMappingDownloadResultWriter);
        when(asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        this.idMappingMessageListener.onMessage(message);

        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.FINISHED, downloadJobResult.getStatus());
        verifyLoggingTotalNoOfEntries(jobRepository, downloadJob, idMappingJob);
    }

    private void verifyLoggingTotalNoOfEntries(
            DownloadJobRepository jobRepository,
            DownloadJob downloadJob,
            IdMappingJob idMappingJob) {
        assertEquals(2, downloadJob.getTotalEntries());
        assertNotNull(downloadJob.getUpdated());
        verify(jobRepository, times(3)).save(downloadJob);
    }

    @Test
    void testOnMessageWithIOExceptionDuringWriteRetryIsCalled() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = getIdMappingDownloadRequest(jobId);
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        // stub
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        String to = "UniProtKB";
        IdMappingJob idMappingJob = getIdMappingJob(to);
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);

        when(this.writerFactory.getResultWriter(to))
                .thenReturn(
                        (AbstractIdMappingDownloadResultWriter)
                                this.uniProtKBIdMappingDownloadResultWriter);

        when(asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);
        String retryQueue = "retry_queue";
        when(asyncDownloadQueueConfigProperties.getRetryQueueName()).thenReturn(retryQueue);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        Mockito.doThrow(new IOException("Forced IO Exception"))
                .when(this.uniProtKBIdMappingDownloadResultWriter)
                .writeResult(any(), any(), any(), any());

        this.idMappingMessageListener.onMessage(message);

        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.ERROR, downloadJobResult.getStatus());
        assertNotNull(downloadJobResult.getUpdated());
        assertEquals(1, downloadJobResult.getRetried());
        assertNotNull(downloadJobResult.getError());
        assertTrue(
                downloadJobResult
                        .getError()
                        .contains(
                                "IdMappingMessageListener.writeResult(IdMappingMessageListener.java"));

        verify(this.rabbitTemplate, atLeast(1)).convertAndSend(eq(retryQueue), any(Message.class));
        verifyLoggingTotalNoOfEntries(jobRepository, downloadJob, idMappingJob);
    }

    @Test
    void testOnMessageWhenMaxRetryReached() {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = getIdMappingDownloadRequest(jobId);
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        String rejectedQueueName = "rejectedQueueNameValue";
        when(asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(0);
        when(asyncDownloadQueueConfigProperties.getRejectedQueueName())
                .thenReturn(rejectedQueueName);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        Assertions.assertDoesNotThrow(() -> this.idMappingMessageListener.onMessage(message));
        Mockito.verify(this.rabbitTemplate, atMostOnce())
                .convertAndSend(eq(rejectedQueueName), any(Message.class));
    }

    @Test
    void testOnMessageWhenIsJobSeenBefore() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = getIdMappingDownloadRequest(jobId);

        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();

        String to = "UniProtKB";
        IdMappingJob idMappingJob = getIdMappingJob(to);
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");

        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));

        Files.createFile(Path.of("target/" + jobId + FileType.GZIP.getExtension()));
        when(asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        Assertions.assertDoesNotThrow(() -> this.idMappingMessageListener.onMessage(message));

        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.FINISHED, downloadJobResult.getStatus());
    }

    @Test
    void testOnMessageWhenIsJobSeenBeforeProcessingDoNothing() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = getIdMappingDownloadRequest(jobId);

        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        String to = "UniProtKB";
        IdMappingJob idMappingJob = getIdMappingJob(to);
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");

        DownloadJob downloadJob = DownloadJob.builder().id(jobId).status(JobStatus.RUNNING).build();
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));

        Files.createFile(Path.of("target/" + jobId + FileType.GZIP.getExtension()));
        when(asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        Assertions.assertDoesNotThrow(() -> this.idMappingMessageListener.onMessage(message));

        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.RUNNING, downloadJobResult.getStatus());
    }

    @Test
    void testOnMessageWhenIdMappingCacheNotFound() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = getIdMappingDownloadRequest(jobId);

        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();

        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        String retryQueueName = "retryQueueNameValue";
        when(asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);
        when(asyncDownloadQueueConfigProperties.getRetryQueueName()).thenReturn(retryQueueName);
        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenThrow(new ResourceNotFoundException("Cache not found"));

        Assertions.assertDoesNotThrow(() -> this.idMappingMessageListener.onMessage(message));

        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.ERROR, downloadJobResult.getStatus());
        assertEquals(1, downloadJobResult.getRetried());
        Mockito.verify(this.rabbitTemplate, atMostOnce())
                .convertAndSend(eq(retryQueueName), any(Message.class));
    }

    private IdMappingDownloadRequestImpl getIdMappingDownloadRequest(String jobId) {
        IdMappingDownloadRequestImpl downloadRequest = new IdMappingDownloadRequestImpl();
        downloadRequest.setJobId(jobId);
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        downloadRequest.setFields("accession,gene_names");
        return downloadRequest;
    }

    private IdMappingJob getIdMappingJob(String to) {
        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedId(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .mappedId(IdMappingStringPair.builder().from("P05067").to("P05067").build())
                        .build();

        IdMappingJobRequest idMappingRequest = new IdMappingJobRequest();
        idMappingRequest.setTo(to);

        return IdMappingJob.builder()
                .idMappingResult(idMappingResult)
                .idMappingRequest(idMappingRequest)
                .jobStatus(JobStatus.FINISHED)
                .created(new Date())
                .build();
    }
}
