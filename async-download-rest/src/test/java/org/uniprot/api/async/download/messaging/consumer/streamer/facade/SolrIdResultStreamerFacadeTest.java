package org.uniprot.api.async.download.messaging.consumer.streamer.facade;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.uniprot.api.async.download.messaging.consumer.streamer.batch.SolrIdBatchResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.messaging.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.messaging.result.common.AsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.job.DownloadJob;
import org.uniprot.api.async.download.model.request.DownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;

public abstract class SolrIdResultStreamerFacadeTest<
        T extends DownloadRequest, R extends DownloadJob, S> {
    public static final String FIELDS = "fields";
    public static final String ID = "Id";
    protected MessageConverterContext<S> messageConverterContext;
    protected RDFResultStreamer<T, R> rdfResultStreamer;
    protected ListResultStreamer<T, R> listResultStreamer;
    protected SolrIdBatchResultStreamer<T, R, S> solrIdBatchResultStreamer;
    protected MessageConverterContextFactory<S> converterContextFactory;
    protected SolrIdResultStreamerFacade<T, R, S> solrIdResultStreamerFacade;
    protected AsyncDownloadFileHandler fileHandler;
    protected T downloadRequest;
    protected Stream<S> entryStream;
    private MockedStatic<Files> filesMockedStatic;
    @Mock
    private Stream<String> ids;
    @Mock
    private Stream<String> rdfStream;
    @Mock
    private Stream<String> listStream;
    @Mock
    private Path path;

    protected void mock() {
        when(downloadRequest.getFields()).thenReturn(FIELDS);
        when(downloadRequest.getId()).thenReturn(ID);
        when(fileHandler.getIdFile(ID)).thenReturn(path);
        filesMockedStatic = mockStatic(Files.class);
        filesMockedStatic.when(() -> Files.lines(path)).thenReturn(ids);
    }

    @AfterEach
    void afterEach() {
        filesMockedStatic.reset();
        filesMockedStatic.close();
    }

    @Test
    void getConvertedResult_rdfType() {
        when(downloadRequest.getFormat()).thenReturn("application/rdf+xml");
        when(converterContextFactory.get(solrIdResultStreamerFacade.getResource(), RDF_MEDIA_TYPE))
                .thenReturn(messageConverterContext);
        when(rdfResultStreamer.stream(downloadRequest, ids)).thenReturn(rdfStream);

        solrIdResultStreamerFacade.getConvertedResult(downloadRequest);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(RDF_MEDIA_TYPE);
        verify(messageConverterContext).setEntityIds(rdfStream);
    }

    @Test
    void getConvertedResult_ListType() {
        when(downloadRequest.getFormat()).thenReturn("text/plain;format=list");
        when(converterContextFactory.get(solrIdResultStreamerFacade.getResource(), LIST_MEDIA_TYPE))
                .thenReturn(messageConverterContext);
        when(listResultStreamer.stream(downloadRequest, ids)).thenReturn(listStream);

        solrIdResultStreamerFacade.getConvertedResult(downloadRequest);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(LIST_MEDIA_TYPE);
        verify(messageConverterContext).setEntityIds(listStream);
    }

    @Test
    void getConvertedResult() {
        when(downloadRequest.getFormat()).thenReturn("application/json");
        when(converterContextFactory.get(solrIdResultStreamerFacade.getResource(), APPLICATION_JSON))
                .thenReturn(messageConverterContext);
        when(solrIdBatchResultStreamer.stream(downloadRequest, ids)).thenReturn(entryStream);

        solrIdResultStreamerFacade.getConvertedResult(downloadRequest);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(APPLICATION_JSON);
        verify(messageConverterContext).setEntities(entryStream);
    }
}
