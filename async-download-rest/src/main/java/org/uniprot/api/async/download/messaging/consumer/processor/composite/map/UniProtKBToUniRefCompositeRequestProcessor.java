package org.uniprot.api.async.download.messaging.consumer.processor.composite.map;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.from.UniProtKBMapToFromIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.result.UniProtKBToUniRefResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.to.UniRefMapToIdRequestProcessor;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;

@Component
public class UniProtKBToUniRefCompositeRequestProcessor
        extends CompositeRequestProcessor<UniProtKBToUniRefDownloadRequest> {
    public UniProtKBToUniRefCompositeRequestProcessor(
            UniProtKBMapToFromIdRequestProcessor uniProtKBMapFromRequestProcessor,
            UniRefMapToIdRequestProcessor uniRefMapToRequestProcessor,
            UniProtKBToUniRefResultRequestProcessor uniRefMapResultRequestProcessor) {
        super(
                List.of(
                        uniProtKBMapFromRequestProcessor,
                        uniRefMapToRequestProcessor,
                        uniRefMapResultRequestProcessor));
    }
}
