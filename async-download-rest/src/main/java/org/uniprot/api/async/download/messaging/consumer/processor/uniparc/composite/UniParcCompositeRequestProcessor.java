package org.uniprot.api.async.download.messaging.consumer.processor.uniparc.composite;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.CompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.id.UniParcSolrIdRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.result.UniParcSolrIdResultRequestProcessor;
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