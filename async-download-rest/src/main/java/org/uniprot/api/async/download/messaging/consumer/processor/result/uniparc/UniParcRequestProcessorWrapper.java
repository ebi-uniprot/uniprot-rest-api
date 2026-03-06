package org.uniprot.api.async.download.messaging.consumer.processor.result.uniparc;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;

@Component
public class UniParcRequestProcessorWrapper implements RequestProcessor<UniParcDownloadRequest> {
    private final UniParcLightSolrIdResultRequestProcessor uniParcLightSolrIdResultRequestProcessor;
    private final UniParcSolrIdResultRequestProcessor uniParcSolrIdResultRequestProcessor;

    public UniParcRequestProcessorWrapper(
            UniParcLightSolrIdResultRequestProcessor uniParcLightSolrIdResultRequestProcessor,
            UniParcSolrIdResultRequestProcessor uniParcSolrIdResultRequestProcessor) {
        this.uniParcLightSolrIdResultRequestProcessor = uniParcLightSolrIdResultRequestProcessor;
        this.uniParcSolrIdResultRequestProcessor = uniParcSolrIdResultRequestProcessor;
    }

    @Override
    public void process(UniParcDownloadRequest request) {
        if (isUniParcFullEntryNeeded(request)) {
            uniParcSolrIdResultRequestProcessor.process(request);
        } else {
            uniParcLightSolrIdResultRequestProcessor.process(request);
        }
    }

    private static boolean isUniParcFullEntryNeeded(UniParcDownloadRequest request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        return MediaType.APPLICATION_XML.equals(contentType)
                || UniProtMediaType.FASTA_EXTENDED_MEDIA_TYPE.equals(contentType);
    }
}
