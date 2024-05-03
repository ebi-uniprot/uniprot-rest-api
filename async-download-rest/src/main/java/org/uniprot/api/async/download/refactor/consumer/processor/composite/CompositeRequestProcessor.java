package org.uniprot.api.async.download.refactor.consumer.processor.composite;

import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;

import java.util.List;

public class CompositeRequestProcessor<T extends DownloadRequest> implements RequestProcessor<T> {
    private final List<RequestProcessor<T>> requestProcessors;

    public CompositeRequestProcessor(List<RequestProcessor<T>> requestProcessors) {
        this.requestProcessors = requestProcessors;
    }

    @Override
    public void process(T request) {
        requestProcessors.forEach(rp-> rp.process(request));
    }
}
