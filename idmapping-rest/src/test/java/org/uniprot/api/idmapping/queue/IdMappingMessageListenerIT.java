package org.uniprot.api.idmapping.queue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
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
import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequestImpl;
import org.uniprot.api.idmapping.model.*;
import org.uniprot.api.idmapping.service.IdMappingJobCacheService;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;

@ExtendWith({MockitoExtension.class})
class IdMappingMessageListenerIT {

    @Mock private MessageConverter converter;

    @Mock DownloadConfigProperties downloadConfigProperties;

    @Mock DownloadJobRepository jobRepository;

    @Mock private RabbitTemplate rabbitTemplate;

    @Mock IdMappingDownloadResultWriterFactory writerFactory;

    @Mock private IdMappingJobCacheService idMappingJobCacheService;

    @Mock UniProtKBIdMappingDownloadResultWriter uniProtKBIdMappingDownloadResultWriter;

    @InjectMocks private IdMappingMessageListener idMappingMessageListener;

    @Test
    void testOnMessageFinishedWithSuccess() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = new IdMappingDownloadRequestImpl();
        downloadRequest.setJobId(jobId);
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        downloadRequest.setFields("accession,gene_names");
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        // stub
        List<String> accessions = List.of("P12345", "P05067");
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedId(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .mappedId(IdMappingStringPair.builder().from("P05067").to("P05067").build())
                        .build();
        IdMappingJobRequest idMappingRequest = new IdMappingJobRequest();
        String to = "uniprotkb";
        idMappingRequest.setTo(to);
        IdMappingJob idMappingJob =
                IdMappingJob.builder()
                        .idMappingResult(idMappingResult)
                        .idMappingRequest(idMappingRequest)
                        .jobStatus(JobStatus.FINISHED)
                        .created(new Date())
                        .build();
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);
        Mockito.verify(this.uniProtKBIdMappingDownloadResultWriter, atMostOnce())
                .writeResult(downloadRequest, idMappingResult, jobId, MediaType.APPLICATION_JSON);
        when(this.writerFactory.getResultWriter(to))
                .thenReturn(
                        (AbstractIdMappingDownloadResultWriter)
                                this.uniProtKBIdMappingDownloadResultWriter);

        ReflectionTestUtils.setField(this.idMappingMessageListener, "maxRetryCount", 3);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        this.idMappingMessageListener.onMessage(message);

        // verify the ids file and clean up
        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.FINISHED, downloadJobResult.getStatus());
    }

    @Test
    void testOnMessageWithIOExceptionDuringWrite() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = new IdMappingDownloadRequestImpl();
        downloadRequest.setJobId(jobId);
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        downloadRequest.setFields("accession,gene_names");
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        // stub
        List<String> accessions = List.of("P12345", "P05067");
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedId(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .mappedId(IdMappingStringPair.builder().from("P05067").to("P05067").build())
                        .build();
        IdMappingJobRequest idMappingRequest = new IdMappingJobRequest();
        String to = "uniprotkb";
        idMappingRequest.setTo(to);
        IdMappingJob idMappingJob =
                IdMappingJob.builder()
                        .idMappingResult(idMappingResult)
                        .idMappingRequest(idMappingRequest)
                        .jobStatus(JobStatus.FINISHED)
                        .created(new Date())
                        .build();
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);

        when(this.writerFactory.getResultWriter(to))
                .thenReturn(
                        (AbstractIdMappingDownloadResultWriter)
                                this.uniProtKBIdMappingDownloadResultWriter);

        ReflectionTestUtils.setField(this.idMappingMessageListener, "maxRetryCount", 3);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        Mockito.doThrow(new IOException("Forced IO Exception"))
                .when(this.uniProtKBIdMappingDownloadResultWriter)
                .writeResult(any(), any(), any(), any());

        this.idMappingMessageListener.onMessage(message);

        // verify the ids file and clean up
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
    }

    @Test
    void testOnMessageWhenMaxRetryReached() {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = new IdMappingDownloadRequestImpl();
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        String rejectedQueueName = "rejectedQueueNameValue";
        ReflectionTestUtils.setField(this.idMappingMessageListener, "maxRetryCount", 0);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);
        ReflectionTestUtils.setField(
                this.idMappingMessageListener, "rejectedQueueName", rejectedQueueName);

        Assertions.assertDoesNotThrow(() -> this.idMappingMessageListener.onMessage(message));
        Mockito.verify(this.rabbitTemplate, atMostOnce())
                .convertAndSend(eq(rejectedQueueName), any(Message.class));
    }

    @Test
    void testOnMessageWhenIsJobSeenBefore() throws IOException {
        String jobId = UUID.randomUUID().toString();
        IdMappingDownloadRequestImpl downloadRequest = new IdMappingDownloadRequestImpl();
        downloadRequest.setJobId(jobId);
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        downloadRequest.setFields("accession,gene_names");

        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();

        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedId(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .mappedId(IdMappingStringPair.builder().from("P05067").to("P05067").build())
                        .build();
        IdMappingJobRequest idMappingRequest = new IdMappingJobRequest();
        String to = "uniprotkb";
        idMappingRequest.setTo(to);
        IdMappingJob idMappingJob =
                IdMappingJob.builder()
                        .idMappingResult(idMappingResult)
                        .idMappingRequest(idMappingRequest)
                        .jobStatus(JobStatus.FINISHED)
                        .created(new Date())
                        .build();
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");

        DownloadJob downloadJob = DownloadJob.builder().id(jobId).build();
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));

        Files.createFile(Path.of("target/" + jobId + FileType.GZIP.getExtension()));
        ReflectionTestUtils.setField(this.idMappingMessageListener, "maxRetryCount", 3);
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
        IdMappingDownloadRequestImpl downloadRequest = new IdMappingDownloadRequestImpl();
        downloadRequest.setJobId(jobId);
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        downloadRequest.setFields("accession,gene_names");

        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();

        IdMappingResult idMappingResult =
                IdMappingResult.builder()
                        .mappedId(IdMappingStringPair.builder().from("P12345").to("P12345").build())
                        .mappedId(IdMappingStringPair.builder().from("P05067").to("P05067").build())
                        .build();
        IdMappingJobRequest idMappingRequest = new IdMappingJobRequest();
        String to = "uniprotkb";
        idMappingRequest.setTo(to);
        IdMappingJob idMappingJob =
                IdMappingJob.builder()
                        .idMappingResult(idMappingResult)
                        .idMappingRequest(idMappingRequest)
                        .jobStatus(JobStatus.FINISHED)
                        .created(new Date())
                        .build();
        when(this.idMappingJobCacheService.getCompletedJobAsResource(jobId))
                .thenReturn(idMappingJob);
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.downloadConfigProperties.getResultFilesFolder()).thenReturn("target");

        DownloadJob downloadJob = DownloadJob.builder().id(jobId).status(JobStatus.RUNNING).build();
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));

        Files.createFile(Path.of("target/" + jobId + FileType.GZIP.getExtension()));
        ReflectionTestUtils.setField(this.idMappingMessageListener, "maxRetryCount", 3);
        ReflectionTestUtils.setField(this.idMappingMessageListener, "writerFactory", writerFactory);

        Assertions.assertDoesNotThrow(() -> this.idMappingMessageListener.onMessage(message));

        Optional<DownloadJob> downloadJobResultOpt = this.jobRepository.findById(jobId);
        assertNotNull(downloadJobResultOpt);
        assertTrue(downloadJobResultOpt.isPresent());
        DownloadJob downloadJobResult = downloadJobResultOpt.get();
        assertEquals(JobStatus.RUNNING, downloadJobResult.getStatus());
    }
}