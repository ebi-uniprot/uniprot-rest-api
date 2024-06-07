package org.uniprot.api.async.download.messaging.consumer.processor.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniref.UniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;

@Component
public class UniRefRequestProcessor implements RequestProcessor<UniRefDownloadRequest> {
    private final UniRefCompositeRequestProcessor uniRefCompositeRequestProcessor;

    public UniRefRequestProcessor(UniRefCompositeRequestProcessor uniRefCompositeRequestProcessor) {
        this.uniRefCompositeRequestProcessor = uniRefCompositeRequestProcessor;
    }

    @Override
    public void process(UniRefDownloadRequest request) {
        uniRefCompositeRequestProcessor.process(request);
    }
}
