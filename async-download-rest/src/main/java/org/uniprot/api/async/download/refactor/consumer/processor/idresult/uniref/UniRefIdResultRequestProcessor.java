package org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniref;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniref.UniRefSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.IdResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniref.UniRefResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniref.UniRefDownloadRequest;
import org.uniprot.core.uniref.UniRefEntryLight;

@Component
public class UniRefIdResultRequestProcessor extends IdResultRequestProcessor<UniRefDownloadRequest, UniRefDownloadJob, UniRefEntryLight> {

    public UniRefIdResultRequestProcessor(UniRefSolrIdRequestProcessor idRequestProcessor, UniRefResultRequestProcessor resultRequestProcessor) {
        super(idRequestProcessor, resultRequestProcessor);
    }
}
