package org.uniprot.api.async.download.refactor.consumer.processor.composite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.InOrderImpl;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;

import java.util.List;

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