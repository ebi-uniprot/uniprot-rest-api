package org.uniprot.api.async.download.messaging.consumer.processor.composite.map;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.from.UniProtKBMapFromRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.result.UniProtKBToUniRefMapResultRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.map.to.UniRefMapToRequestProcessor;
import org.uniprot.api.async.download.model.request.map.UniProtKBToUniRefMapDownloadRequest;

@Component
public class UniProtKBToUniRefMapCompositeRequestProcessor
        extends CompositeRequestProcessor<UniProtKBToUniRefMapDownloadRequest> {
    public UniProtKBToUniRefMapCompositeRequestProcessor(
            UniProtKBMapFromRequestProcessor uniProtKBMapFromRequestProcessor,
            UniRefMapToRequestProcessor uniRefMapToRequestProcessor,
            UniProtKBToUniRefMapResultRequestProcessor uniRefMapResultRequestProcessor) {
        super(
                List.of(
                        uniProtKBMapFromRequestProcessor,
                        uniRefMapToRequestProcessor,
                        uniRefMapResultRequestProcessor));
    }
}
