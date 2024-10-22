package org.uniprot.api.async.download.messaging.consumer.processor.uniref.composite;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniref.id.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniref.result.UniRefSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;

@Component
public class UniRefCompositeRequestProcessor
        extends CompositeRequestProcessor<UniRefDownloadRequest> {

    public UniRefCompositeRequestProcessor(
            UniRefSolrIdRequestProcessor uniRefSolrIdRequestProcessor,
            UniRefSolrIdResultRequestProcessor uniRefResultRequestProcessor) {
        super(List.of(uniRefSolrIdRequestProcessor, uniRefResultRequestProcessor));
    }
}
