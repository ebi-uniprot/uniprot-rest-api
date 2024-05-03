package org.uniprot.api.async.download.refactor.consumer.processor.composite.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniref.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniref.UniRefResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;

import java.util.List;

@Component
public class UniRefCompositeRequestProcessor extends CompositeRequestProcessor<UniRefDownloadRequest> {

    public UniRefCompositeRequestProcessor(UniRefSolrIdRequestProcessor uniRefSolrIdRequestProcessor, UniRefResultRequestProcessor uniRefResultRequestProcessor) {
        super(List.of(uniRefSolrIdRequestProcessor,uniRefResultRequestProcessor));
    }
}
