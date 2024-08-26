package org.uniprot.api.async.download.messaging.consumer.processor.composite.map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.from.UniProtKBMapFromRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.result.UniProtKBToUniRefMapResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.to.UniRefMapToRequestProcessor;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;

import java.util.List;

@Component
public class UniProtKBToUniRefMapCompositeRequestProcessor extends CompositeRequestProcessor<UniProtKBMapDownloadRequest> {
    public UniProtKBToUniRefMapCompositeRequestProcessor(
            UniProtKBMapFromRequestProcessor uniProtKBMapFromRequestProcessor,
            UniRefMapToRequestProcessor uniRefMapToRequestProcessor,
            UniProtKBToUniRefMapResultRequestProcessor uniRefMapResultRequestProcessor
    ) {
        super(List.of(uniProtKBMapFromRequestProcessor,uniRefMapToRequestProcessor, uniRefMapResultRequestProcessor));
    }
}
