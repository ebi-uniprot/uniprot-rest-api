package org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb.UniProtKBResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.IdResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

@Component
public class UniProtKBIdResultRequestProcessor extends IdResultRequestProcessor<UniProtKBDownloadRequest, UniProtKBDownloadJob, UniProtKBEntry> {

    public UniProtKBIdResultRequestProcessor(UniProtKBSolrIdRequestProcessor idRequestProcessor, UniProtKBResultRequestProcessor resultRequestProcessor) {
        super(idRequestProcessor, resultRequestProcessor);
    }
}
