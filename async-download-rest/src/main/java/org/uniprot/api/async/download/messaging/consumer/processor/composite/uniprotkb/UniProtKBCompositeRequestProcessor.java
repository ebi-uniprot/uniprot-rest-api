package org.uniprot.api.async.download.messaging.consumer.processor.composite.uniprotkb;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb.UniProtKBSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniprotkb.UniProtKBSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;

@Component
public class UniProtKBCompositeRequestProcessor
        extends CompositeRequestProcessor<UniProtKBDownloadRequest> {

    public UniProtKBCompositeRequestProcessor(
            UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor,
            UniProtKBSolrIdResultRequestProcessor uniProtKBResultRequestProcessor) {
        super(List.of(uniProtKBSolrIdRequestProcessor, uniProtKBResultRequestProcessor));
    }
}
