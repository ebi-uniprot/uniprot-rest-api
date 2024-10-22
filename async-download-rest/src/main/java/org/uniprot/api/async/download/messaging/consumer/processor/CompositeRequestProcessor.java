package org.uniprot.api.async.download.messaging.consumer.processor;

import java.util.List;

import org.uniprot.api.async.download.model.request.DownloadRequest;

public class CompositeRequestProcessor<T extends DownloadRequest> implements RequestProcessor<T> {
    private final List<RequestProcessor<T>> requestProcessors;

    public CompositeRequestProcessor(List<RequestProcessor<T>> requestProcessors) {
        this.requestProcessors = requestProcessors;
    }

    @Override
    public void process(T request) {
        requestProcessors.forEach(rp -> rp.process(request));
    }
}
