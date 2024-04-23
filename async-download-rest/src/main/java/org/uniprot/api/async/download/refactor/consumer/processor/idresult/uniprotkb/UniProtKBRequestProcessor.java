package org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniprotkb;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb.UniProtKBResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBH5EnabledSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.CompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;

import java.util.List;

@Component
public class UniProtKBRequestProcessor extends CompositeRequestProcessor<UniProtKBDownloadRequest> {

    public UniProtKBRequestProcessor(UniProtKBH5EnabledSolrIdRequestProcessor idRequestProcessor, UniProtKBResultRequestProcessor resultRequestProcessor) {
        super(List.of(idRequestProcessor, resultRequestProcessor));
    }

    //todo add h5 separation code

}
