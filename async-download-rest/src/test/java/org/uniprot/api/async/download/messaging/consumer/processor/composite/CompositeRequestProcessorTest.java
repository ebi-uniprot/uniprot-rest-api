package org.uniprot.api.async.download.messaging.consumer.processor.composite;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.internal.InOrderImpl;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.model.request.DownloadRequest;

public abstract class CompositeRequestProcessorTest<T extends DownloadRequest> {
    protected RequestProcessor<T> requestProcessor1;
    protected RequestProcessor<T> requestProcessor2;
    protected T downloadRequest;
    protected CompositeRequestProcessor<T> compositeRequestProcessor;

    @Test
    void process() {
        compositeRequestProcessor.process(downloadRequest);

        InOrderImpl inOrder = new InOrderImpl(List.of(requestProcessor1, requestProcessor2));

        inOrder.verify(requestProcessor1).process(downloadRequest);
        inOrder.verify(requestProcessor2).process(downloadRequest);
    }
}
