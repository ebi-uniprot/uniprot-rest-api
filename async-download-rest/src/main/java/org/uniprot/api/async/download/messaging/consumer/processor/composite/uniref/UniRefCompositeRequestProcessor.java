package org.uniprot.api.async.download.messaging.consumer.processor.composite.uniref;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniref.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniref.UniRefSolrIdResultRequestProcessor;
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
