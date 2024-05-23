package org.uniprot.api.async.download.refactor.consumer.processor.result;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.uniprot.api.async.download.messaging.config.idmapping.IdMappingDownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.idmapping.IdMappingHeartbeatProducer;
import org.uniprot.api.async.download.refactor.consumer.streamer.facade.IdMappingResultStreamerFacade;
import org.uniprot.api.async.download.refactor.request.idmapping.IdMappingDownloadRequest;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractUUWHttpMessageConverter;
import org.uniprot.api.rest.output.converter.UUWMessageConverterFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public abstract class IdMappingResultRequestProcessorTest<Q, P extends EntryPair<Q>> {
    public static final String CONTENT_TYPE = "application/json";
    public static final String JOB_ID = "someJobId";
    public static final String RESULT_FOLDER = "resultFolder";
    @Mock
    protected IdMappingDownloadRequest request;
    protected IdMappingDownloadConfigProperties downloadConfigProperties;
    protected IdMappingHeartbeatProducer heartbeatProducer;
    protected IdMappingResultStreamerFacade<Q,P> solrIdResultStreamerFacade;
    protected IdMappingResultRequestProcessor<Q,P> solrIdResultRequestProcessor;
    protected UUWMessageConverterFactory messageConverterFactory;
    @Mock private AbstractUUWHttpMessageConverter outputWriter;
    @Mock private MessageConverterContext<P> context;
    @Mock private Path resultPath;
    @Mock private OutputStream outputStream;
    @Mock private Instant instant;

    @Test
    void process() throws Exception {
        when(request.getFormat()).thenReturn(CONTENT_TYPE);
        when(request.getJobId()).thenReturn(JOB_ID);
        when(messageConverterFactory.getOutputWriter(
                        MediaType.APPLICATION_JSON, solrIdResultRequestProcessor.getType()))
                .thenReturn(outputWriter);
        when(solrIdResultStreamerFacade.getConvertedResult(request)).thenReturn(context);
        when(downloadConfigProperties.getResultFilesFolder()).thenReturn(RESULT_FOLDER);
        MockedStatic<Files> filesMockedStatic = mockStatic(Files.class);
        filesMockedStatic
                .when(
                        () ->
                                Files.newOutputStream(
                                        resultPath,
                                        StandardOpenOption.WRITE,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.TRUNCATE_EXISTING))
                .thenReturn(outputStream);
        MockedStatic<Paths> pathsMockedStatic = mockStatic(Paths.class);
        pathsMockedStatic
                .when(() -> Paths.get(RESULT_FOLDER, JOB_ID + FileType.GZIP.getExtension()))
                .thenReturn(resultPath);
        MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class);
        instantMockedStatic.when(() -> Instant.now()).thenReturn(instant);
        MockedConstruction<GZIPOutputStream> gzipOutputStreamMockedConstruction =
                Mockito.mockConstruction(GZIPOutputStream.class, (mock, context) -> {});
        MockedConstruction<AtomicInteger> atomicIntegerMockedConstruction =
                Mockito.mockConstruction(AtomicInteger.class, (mock, context) -> {});

        solrIdResultRequestProcessor.process(request);

        verify(outputWriter)
                .writeContents(
                        context,
                        gzipOutputStreamMockedConstruction.constructed().get(0),
                        instant,
                        atomicIntegerMockedConstruction.constructed().get(0));
        verify(heartbeatProducer).stop(JOB_ID);

        filesMockedStatic.reset();
        filesMockedStatic.close();
        pathsMockedStatic.reset();
        pathsMockedStatic.close();
        instantMockedStatic.reset();
        instantMockedStatic.close();
        gzipOutputStreamMockedConstruction.close();
        atomicIntegerMockedConstruction.close();
    }

    @Test
    void onMessage_whenExceptionOccurred() {
        when(request.getFormat()).thenReturn(CONTENT_TYPE);
        when(request.getJobId()).thenReturn(JOB_ID);
        when(downloadConfigProperties.getResultFilesFolder()).thenThrow(new RuntimeException());

        assertThrows(
                ResultProcessingException.class, () -> solrIdResultRequestProcessor.process(request));

        verify(heartbeatProducer).stop(JOB_ID);
    }
}
