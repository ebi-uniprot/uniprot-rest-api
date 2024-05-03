package org.uniprot.api.async.download.refactor.consumer.processor.composite.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb.UniProtKBResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;

import java.util.List;

@Component
public class UniProtKBCompositeRequestProcessor extends CompositeRequestProcessor<UniProtKBDownloadRequest> {

    public UniProtKBCompositeRequestProcessor(UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor, UniProtKBResultRequestProcessor uniProtKBResultRequestProcessor) {
        super(List.of(uniProtKBSolrIdRequestProcessor,uniProtKBResultRequestProcessor));
    }
}
