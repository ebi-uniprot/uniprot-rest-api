package org.uniprot.api.async.download.messaging.consumer.processor.composite.uniparc;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniparc.UniParcSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc.UniParcSolrIdResultRequestProcessor;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;

@Component
public class UniParcCompositeRequestProcessor
        extends CompositeRequestProcessor<UniParcDownloadRequest> {

    public UniParcCompositeRequestProcessor(
            UniParcSolrIdRequestProcessor uniParcSolrIdRequestProcessor,
            UniParcSolrIdResultRequestProcessor uniParcSolrIdResultRequestProcessor) {
        super(List.of(uniParcSolrIdRequestProcessor, uniParcSolrIdResultRequestProcessor));
    }
}
