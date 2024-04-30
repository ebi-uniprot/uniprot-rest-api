package org.uniprot.api.async.download.refactor.consumer.processor.idresult.uniprotkb;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdH5RequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.id.uniprotkb.UniProtKBSolrIdRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.idresult.CompositeRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.uniprotkb.UniProtKBResultRequestProcessor;
import org.uniprot.api.async.download.refactor.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.rest.output.UniProtMediaType;

import java.util.List;

@Component
public class UniProtKBRequestProcessor extends CompositeRequestProcessor<UniProtKBDownloadRequest> {

    private final UniProtKBSolrIdH5RequestProcessor uniProtKBSolrIdH5RequestProcessor;

    public UniProtKBRequestProcessor(UniProtKBSolrIdRequestProcessor idRequestProcessor, UniProtKBSolrIdH5RequestProcessor uniProtKBSolrIdH5RequestProcessor, UniProtKBResultRequestProcessor resultRequestProcessor) {
        super(List.of(idRequestProcessor, resultRequestProcessor));
        this.uniProtKBSolrIdH5RequestProcessor = uniProtKBSolrIdH5RequestProcessor;
    }

    @Override
    public void process(UniProtKBDownloadRequest request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());

        if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
            uniProtKBSolrIdH5RequestProcessor.process(request);
        } else {
            super.process(request);
        }
    }
}
