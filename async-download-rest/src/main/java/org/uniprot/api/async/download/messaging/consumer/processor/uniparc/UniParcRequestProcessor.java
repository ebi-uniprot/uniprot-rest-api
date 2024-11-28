package org.uniprot.api.async.download.messaging.consumer.processor.uniparc;

import static org.uniprot.api.async.download.messaging.repository.JobFields.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.uniparc.composite.UniParcCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.uniparc.UniParcDownloadRequest;
import org.uniprot.api.async.download.service.uniparc.UniParcJobService;

@Component
public class UniParcRequestProcessor implements RequestProcessor<UniParcDownloadRequest> {
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
        jobService.update(request.getDownloadJobId(), Map.of(STATUS.getName(), RUNNING));

        uniParcCompositeRequestProcessor.process(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(
                        STATUS.getName(),
                        FINISHED,
                        RESULT_FILE.getName(),
                        request.getDownloadJobId()));
    }
}
