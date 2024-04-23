package org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniref.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.CompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniref.UniRefResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;

import java.util.List;

@Component
public class UniRefRequestProcessor extends CompositeRequestProcessor<UniRefDownloadRequest> {

    public UniRefRequestProcessor(UniRefSolrIdRequestProcessor idRequestProcessor, UniRefResultRequestProcessor resultRequestProcessor) {
        super(List.of(idRequestProcessor,resultRequestProcessor));
    }
}
