package org.uniprot.api.async.download.messaging.consumer.processor.composite.mapto;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.from.UniProtKBToUniRefFromIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.result.UniProtKBToUniRefResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.to.UniProtKBToUniRefToIdRequestProcessor;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;

@Component
public class UniProtKBToUniRefCompositeRequestProcessor
        extends CompositeRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    public UniProtKBToUniRefCompositeRequestProcessor(
            UniProtKBToUniRefFromIdRequestProcessor uniProtKBMapFromRequestProcessor,
            UniProtKBToUniRefToIdRequestProcessor uniRefMapToRequestProcessor,
            UniProtKBToUniRefResultRequestProcessor uniRefMapResultRequestProcessor) {
        super(
                List.of(
                        uniProtKBMapFromRequestProcessor,
                        uniRefMapToRequestProcessor,
                        uniRefMapResultRequestProcessor));
    }
}
