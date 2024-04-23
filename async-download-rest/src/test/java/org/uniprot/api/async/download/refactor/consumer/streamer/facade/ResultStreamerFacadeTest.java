package org.uniprot.api.async.download.refactor.consumer.streamer.facade;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.uniprot.api.async.download.model.common.DownloadJob;
import org.uniprot.api.async.download.refactor.consumer.streamer.batch.BatchResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.list.ListResultStreamer;
import org.uniprot.api.async.download.refactor.consumer.streamer.rdf.RDFResultStreamer;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.RDF_MEDIA_TYPE;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource;

public abstract class ResultStreamerFacadeTest<T extends DownloadRequest, R extends DownloadJob, S> {
    public static final String FIELDS = "fields";
    protected MessageConverterContext<S> messageConverterContext;
    protected RDFResultStreamer<T, R> rdfResultStreamer;
    protected ListResultStreamer<T, R> listResultStreamer;
    protected BatchResultStreamer<T, R, S> batchResultStreamer;
    protected MessageConverterContextFactory<S> converterContextFactory;
    protected ResultStreamerFacade<T, R, S> resultStreamerFacade;
    protected T downloadRequest;
    protected Stream<S> entryStream;
    @Mock
    private Stream<String> ids;
    @Mock
    private Stream<String> rdfStream;
    @Mock
    private Stream<String> listStream;

    protected void mock() {
        when(downloadRequest.getFields()).thenReturn(FIELDS);
    }

    @Test
    void getConvertedResult_rdfType() {
        when(downloadRequest.getFormat()).thenReturn("application/rdf+xml");
        when(converterContextFactory.get(getResource(), RDF_MEDIA_TYPE)).thenReturn(messageConverterContext);
        when(rdfResultStreamer.stream(downloadRequest,ids)).thenReturn(rdfStream);

        resultStreamerFacade.getConvertedResult(downloadRequest, ids);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(RDF_MEDIA_TYPE);
        verify(messageConverterContext).setEntityIds(rdfStream);
    }

    @Test
    void getConvertedResult_ListType() {
        when(downloadRequest.getFormat()).thenReturn("text/plain;format=list");
        when(converterContextFactory.get(getResource(), LIST_MEDIA_TYPE)).thenReturn(messageConverterContext);
        when(listResultStreamer.stream(downloadRequest,ids)).thenReturn(listStream);

        resultStreamerFacade.getConvertedResult(downloadRequest, ids);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(LIST_MEDIA_TYPE);
        verify(messageConverterContext).setEntityIds(listStream);
    }

    @Test
    void getConvertedResult() {
        when(downloadRequest.getFormat()).thenReturn("application/json");
        when(converterContextFactory.get(getResource(), APPLICATION_JSON)).thenReturn(messageConverterContext);
        when(batchResultStreamer.stream(downloadRequest,ids)).thenReturn(entryStream);

        resultStreamerFacade.getConvertedResult(downloadRequest, ids);

        verify(messageConverterContext).setFields(FIELDS);
        verify(messageConverterContext).setContentType(APPLICATION_JSON);
        verify(messageConverterContext).setEntities(entryStream);
    }

    protected abstract Resource getResource();
}