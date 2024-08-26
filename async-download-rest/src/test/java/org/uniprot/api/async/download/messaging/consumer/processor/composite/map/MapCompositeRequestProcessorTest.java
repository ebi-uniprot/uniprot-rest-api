package org.uniprot.api.async.download.messaging.consumer.processor.composite.map;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.internal.InOrderImpl;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;

public abstract class MapCompositeRequestProcessorTest<T extends MapDownloadRequest> {
    protected RequestProcessor<T> requestProcessor1;
    protected RequestProcessor<T> requestProcessor2;
    protected RequestProcessor<T> requestProcessor3;
    protected T downloadRequest;
    protected CompositeRequestProcessor<T> compositeRequestProcessor;

    @Test
    void process() {
        compositeRequestProcessor.process(downloadRequest);

        InOrderImpl inOrder =
                new InOrderImpl(List.of(requestProcessor1, requestProcessor2, requestProcessor3));

        inOrder.verify(requestProcessor1).process(downloadRequest);
        inOrder.verify(requestProcessor2).process(downloadRequest);
        inOrder.verify(requestProcessor3).process(downloadRequest);
    }
}
