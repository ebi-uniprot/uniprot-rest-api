package org.uniprot.api.async.download.refactor.consumer.processor.uniprotkb;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.composite.uniprotkb.UniProtKBCompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdHD5RequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;

@Component
public class UniProtKBRequestProcessor implements RequestProcessor<UniProtKBDownloadRequest> {

    private final UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor;
    private final UniProtKBCompositeRequestProcessor uniProtKBCompositeRequestProcessor;

    public UniProtKBRequestProcessor(UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor, UniProtKBCompositeRequestProcessor uniProtKBCompositeRequestProcessor) {
        this.uniProtKBSolrIdHD5RequestProcessor = uniProtKBSolrIdHD5RequestProcessor;
        this.uniProtKBCompositeRequestProcessor = uniProtKBCompositeRequestProcessor;
    }

    @Override
    public void process(UniProtKBDownloadRequest request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

        if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
            uniProtKBSolrIdHD5RequestProcessor.process(request);
        } else {
            uniProtKBCompositeRequestProcessor.process(request);
        }
    }
}
