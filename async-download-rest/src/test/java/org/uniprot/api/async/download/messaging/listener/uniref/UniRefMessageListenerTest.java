package org.uniprot.api.async.download.messaging.listener.uniref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefAsyncDownloadQueueConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;
import org.uniprot.api.async.download.messaging.listener.common.BaseAbstractMessageListener;
import org.uniprot.api.async.download.messaging.listener.common.MessageListenerException;
import org.uniprot.api.async.download.messaging.repository.DownloadJobRepository;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefDownloadResultWriter;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadRequest;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightService;
import org.uniprot.core.uniref.UniRefEntryLight;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class UniRefMessageListenerTest {

    private static final String UPDATE_COUNT = "updateCount";
    private static final String PROCESSED_ENTRIES = "processedEntries";
    @Mock private MessageConverter converter;
    @Mock private UniRefEntryLightService service;
    @Mock private UniRefDownloadConfigProperties uniRefDownloadConfigProperties;

    @Mock UniRefAsyncDownloadQueueConfigProperties asyncDownloadQueueConfigProperties;

    @Mock private UniRefDownloadJobRepository jobRepository;

    @Mock private UniRefDownloadResultWriter uniRefDownloadResultWriter;

    @Mock private UniRefRabbitTemplate uniRefRabbitTemplate;
    @Mock private UniRefHeartbeatProducer heartbeatProducer;
    @Mock private UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler;

    @InjectMocks private UniRefMessageListener uniRefMessageListener;

    @Test
    void testOnMessage() throws IOException {
        UniRefDownloadRequest downloadRequest = new UniRefDownloadRequest();
        downloadRequest.setQuery("field:value");
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        String jobId = UUID.randomUUID().toString();
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        UniRefDownloadJob downloadJob = UniRefDownloadJob.builder().id(jobId).build();
        // stub
        List<String> accessions = List.of("UniRef90_P03904", "UniRef90_P03903");
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.uniRefDownloadConfigProperties.getIdFilesFolder()).thenReturn("target");
        when(this.uniRefDownloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        when(this.service.streamIdsForDownload(downloadRequest)).thenReturn(accessions.stream());
        when(this.service.search(any(SearchRequest.class)))
                .thenReturn(
                        QueryResult.<UniRefEntryLight>builder()
                                .page(CursorPage.of("", 10, 2))
                                .build());
        when(this.asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);

        this.uniRefMessageListener.onMessage(message);

        // verify the ids file and clean up
        Path idsFilePath = Path.of("target/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertEquals(accessions.size(), ids.size());
        Assertions.assertEquals(accessions, ids);
        Files.delete(idsFilePath);
        Assertions.assertTrue(Files.notExists(idsFilePath));
        verifyLoggingTotalNoOfEntries(jobRepository, downloadJob);
        verify(heartbeatProducer, atLeastOnce()).createForIds(same(downloadJob));
        verify(heartbeatProducer).stop(jobId);
    }

    @Test
    void testOnMessageWhenRetry() throws IOException {
        UniRefDownloadRequest downloadRequest = new UniRefDownloadRequest();
        downloadRequest.setQuery("field:value");
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        String jobId = UUID.randomUUID().toString();
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        UniRefDownloadJob downloadJob = UniRefDownloadJob.builder().id(jobId).build();
        downloadJob.setRetried(1);
        // stub
        List<String> accessions = List.of("UniRef90_P03904", "UniRef90_P03903");
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.uniRefDownloadConfigProperties.getIdFilesFolder()).thenReturn("target");
        when(this.uniRefDownloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        when(this.service.streamIdsForDownload(downloadRequest)).thenReturn(accessions.stream());
        when(this.service.search(any(SearchRequest.class)))
                .thenReturn(
                        QueryResult.<UniRefEntryLight>builder()
                                .page(CursorPage.of("", 10, 2))
                                .build());
        when(this.asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);

        this.uniRefMessageListener.onMessage(message);

        // verify the ids file and clean up
        verifyCleanFilesAndResetCounts(jobId);
        Path idsFilePath = Path.of("target/" + jobId);
        Assertions.assertTrue(Files.exists(idsFilePath));
        List<String> ids = Files.readAllLines(idsFilePath);
        Assertions.assertNotNull(ids);
        Assertions.assertEquals(accessions.size(), ids.size());
        Assertions.assertEquals(accessions, ids);
        Files.delete(idsFilePath);
        Assertions.assertTrue(Files.notExists(idsFilePath));
        verifyLoggingTotalNoOfEntries(jobRepository, downloadJob);
        verify(heartbeatProducer, atLeastOnce()).createForIds(same(downloadJob));
        verify(heartbeatProducer).stop(jobId);
    }

    private void verifyCleanFilesAndResetCounts(String jobId) {
        verify(uniRefAsyncDownloadFileHandler).deleteAllFiles(jobId);
        verify(jobRepository)
                .update(
                        eq(jobId),
                        argThat(
                                map ->
                                        Objects.equals(0L, map.get(UPDATE_COUNT))
                                                && Objects.equals(map.get(PROCESSED_ENTRIES), 0L)));
    }

    private void verifyLoggingTotalNoOfEntries(
            DownloadJobRepository jobRepository, DownloadJob downloadJob) {
        assertEquals(2, downloadJob.getTotalEntries());
        assertNotNull(downloadJob.getUpdated());
        verify(jobRepository, times(3)).save(downloadJob);
    }

    @Test
    void testOnMessageWithIOExceptionDuringWrite() throws IOException {
        UniRefDownloadRequest downloadRequest = new UniRefDownloadRequest();
        downloadRequest.setQuery("field2:value2");
        MediaType format = MediaType.APPLICATION_JSON;
        downloadRequest.setFormat(format.toString());
        String jobId = UUID.randomUUID().toString();
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        UniRefDownloadJob downloadJob = UniRefDownloadJob.builder().id(jobId).build();
        // stub
        List<String> accessions = List.of("UniRef90_P03904", "UniRef90_P03903");
        when(this.jobRepository.findById(jobId)).thenReturn(Optional.of(downloadJob));
        when(this.converter.fromMessage(message)).thenReturn(downloadRequest);
        when(this.uniRefDownloadConfigProperties.getIdFilesFolder()).thenReturn("target");
        when(this.uniRefDownloadConfigProperties.getResultFilesFolder()).thenReturn("target");
        when(this.service.streamIdsForDownload(downloadRequest)).thenReturn(accessions.stream());
        when(this.service.search(any(SearchRequest.class)))
                .thenReturn(
                        QueryResult.<UniRefEntryLight>builder()
                                .page(CursorPage.of("", 10, 2))
                                .build());
        Mockito.doThrow(new IOException("Forced IO Exception"))
                .when(this.uniRefDownloadResultWriter)
                .writeResult(any(), any(), any(), any(), any(), any());
        when(this.asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(3);

        this.uniRefMessageListener.onMessage(message);

        verifyLoggingTotalNoOfEntries(jobRepository, downloadJob);
        verify(heartbeatProducer, atLeastOnce()).createForIds(same(downloadJob));
        verify(heartbeatProducer).stop(jobId);
    }

    @Test
    void testOnMessageWhenMaxRetryReached() throws IOException {
        UniRefDownloadRequest downloadRequest = new UniRefDownloadRequest();
        downloadRequest.setQuery("field1:value1");
        downloadRequest.setFormat(MediaType.APPLICATION_JSON.toString());
        String jobId = UUID.randomUUID().toString();
        MessageBuilder builder = MessageBuilder.withBody(downloadRequest.toString().getBytes());
        Message message = builder.setHeader("jobId", jobId).build();
        when(this.asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(0);
        Assertions.assertDoesNotThrow(() -> this.uniRefMessageListener.onMessage(message));
        verifyCleanFilesAndResetCounts(jobId);
    }

    @Test
    void testOnMessageWhenExceptionIsThrown() {
        Message message =
                MessageBuilder.withBody("test".getBytes()).setHeader("jobId", "12345").build();
        when(this.asyncDownloadQueueConfigProperties.getRetryMaxCount()).thenReturn(0);
        Assertions.assertDoesNotThrow(() -> this.uniRefMessageListener.onMessage(message));
    }

    @Test
    void testAddAdditionalHeaders() {
        Message message =
                MessageBuilder.withBody("test".getBytes()).setHeader("jobId", "12345").build();
        MessageListenerException mle =
                new MessageListenerException("This is a test exception messsage");
        Message messageWithHeaders = this.uniRefMessageListener.addAdditionalHeaders(message, mle);
        Assertions.assertNotNull(messageWithHeaders);
        Assertions.assertEquals(message.getBody(), messageWithHeaders.getBody());
        Assertions.assertEquals(1, message.getMessageProperties().getHeaders().size());
        Assertions.assertEquals(
                (String) message.getMessageProperties().getHeader("jobId"),
                messageWithHeaders.getMessageProperties().getHeader("jobId"));
        // newly added headers
        Assertions.assertEquals(3, messageWithHeaders.getMessageProperties().getHeaders().size());
        Assertions.assertEquals(
                1,
                (Integer)
                        messageWithHeaders
                                .getMessageProperties()
                                .getHeader(
                                        BaseAbstractMessageListener.CURRENT_RETRIED_COUNT_HEADER));
        Assertions.assertNotNull(
                messageWithHeaders
                        .getMessageProperties()
                        .getHeader(BaseAbstractMessageListener.CURRENT_RETRIED_ERROR_HEADER));
    }
}
