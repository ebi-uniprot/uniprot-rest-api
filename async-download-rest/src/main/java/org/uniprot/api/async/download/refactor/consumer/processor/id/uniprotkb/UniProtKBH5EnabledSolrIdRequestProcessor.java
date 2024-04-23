package org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.async.download.refactor.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.SolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;

@Component
public class UniProtKBH5EnabledSolrIdRequestProcessor implements IdRequestProcessor<UniProtKBDownloadRequest> {
    private final UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor;
    private final UniProtKBSolrIdH5RequestProcessor uniProtKBSolrIdH5RequestProcessor;

    public UniProtKBH5EnabledSolrIdRequestProcessor(UniProtKBSolrIdRequestProcessor uniProtKBSolrIdRequestProcessor, UniProtKBSolrIdH5RequestProcessor uniProtKBSolrIdH5RequestProcessor) {
        this.uniProtKBSolrIdRequestProcessor = uniProtKBSolrIdRequestProcessor;
        this.uniProtKBSolrIdH5RequestProcessor = uniProtKBSolrIdH5RequestProcessor;
    }

    @Override
    public void process(UniProtKBDownloadRequest request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        getRequestProcessor(contentType).process(request);
    }

    private SolrIdRequestProcessor<UniProtKBDownloadRequest, UniProtKBDownloadJob> getRequestProcessor(MediaType contentType) {
        if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
            return uniProtKBSolrIdH5RequestProcessor;
        }
        return uniProtKBSolrIdRequestProcessor;
    }

}
