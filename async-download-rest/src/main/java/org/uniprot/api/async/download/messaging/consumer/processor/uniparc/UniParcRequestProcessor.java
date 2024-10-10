package org.uniprot.api.async.download.messaging.consumer.processor.uniparc;

import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniparc.UniParcCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class UniParcRequestProcessor implements RequestProcessor<UniParcDownloadRequest> {
    protected static final String RESULT_FILE = "resultFile";
    protected static final String STATUS = "status";
    private final UniParcCompositeRequestProcessor uniParcCompositeRequestProcessor;
    private final UniParcJobService jobService;

    public UniParcRequestProcessor(
            UniParcCompositeRequestProcessor uniParcCompositeRequestProcessor,
            UniParcJobService jobService) {
        this.uniParcCompositeRequestProcessor = uniParcCompositeRequestProcessor;
        this.jobService = jobService;
    }

    @Override
    public void process(UniParcDownloadRequest request) {
        jobService.update(request.getDownloadJobId(), Map.of(STATUS, RUNNING));

        uniParcCompositeRequestProcessor.process(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(STATUS, FINISHED, RESULT_FILE, request.getDownloadJobId()));
    }
}
