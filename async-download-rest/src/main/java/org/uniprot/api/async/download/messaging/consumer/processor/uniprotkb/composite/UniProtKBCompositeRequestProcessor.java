package org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.composite;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.id.UniProtKBSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb.result.UniProtKBSolrIdResultRequestProcessor;
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
