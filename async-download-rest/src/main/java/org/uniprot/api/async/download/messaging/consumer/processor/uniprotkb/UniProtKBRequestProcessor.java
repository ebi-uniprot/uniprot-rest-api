package org.uniprot.api.async.download.messaging.consumer.processor.uniprotkb;

import static org.uniprot.api.rest.download.model.JobStatus.*;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniprotkb.UniProtKBCompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.id.uniprotkb.UniProtKBSolrIdHD5RequestProcessor;
import org.uniprot.api.async.download.model.request.uniprotkb.UniProtKBDownloadRequest;
import org.uniprot.api.async.download.service.uniprotkb.UniProtKBJobService;
import org.uniprot.api.rest.output.UniProtMediaType;

@Component
public class UniProtKBRequestProcessor implements RequestProcessor<UniProtKBDownloadRequest> {
    protected static final String RESULT_FILE = "resultFile";
    protected static final String STATUS = "status";
    private final UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor;
    private final UniProtKBCompositeRequestProcessor uniProtKBCompositeRequestProcessor;
    private final UniProtKBJobService jobService;

    public UniProtKBRequestProcessor(
            UniProtKBSolrIdHD5RequestProcessor uniProtKBSolrIdHD5RequestProcessor,
            UniProtKBCompositeRequestProcessor uniProtKBCompositeRequestProcessor,
            UniProtKBJobService jobService) {
        this.uniProtKBSolrIdHD5RequestProcessor = uniProtKBSolrIdHD5RequestProcessor;
        this.uniProtKBCompositeRequestProcessor = uniProtKBCompositeRequestProcessor;
        this.jobService = jobService;
    }

    @Override
    public void process(UniProtKBDownloadRequest request) {
        MediaType contentType = UniProtMediaType.valueOf(request.getFormat());
        jobService.update(request.getDownloadJobId(), Map.of(STATUS, RUNNING));

        if (UniProtMediaType.HDF5_MEDIA_TYPE.equals(contentType)) {
            uniProtKBSolrIdHD5RequestProcessor.process(request);
        } else {
            uniProtKBCompositeRequestProcessor.process(request);
            jobService.update(
                    request.getDownloadJobId(),
                    Map.of(STATUS, FINISHED, RESULT_FILE, request.getDownloadJobId()));
        }
    }
}
