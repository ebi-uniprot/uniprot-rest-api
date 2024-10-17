package org.uniprot.api.async.download.messaging.consumer.processor.uniref;

import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.uniref.UniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.messaging.repository.JobFields;
import org.uniprot.api.async.download.model.request.uniref.UniRefDownloadRequest;
import org.uniprot.api.async.download.service.uniref.UniRefJobService;

@Component
public class UniRefRequestProcessor implements RequestProcessor<UniRefDownloadRequest> {
    private final UniRefCompositeRequestProcessor uniRefCompositeRequestProcessor;
    private final UniRefJobService jobService;

    public UniRefRequestProcessor(
            UniRefCompositeRequestProcessor uniRefCompositeRequestProcessor,
            UniRefJobService jobService) {
        this.uniRefCompositeRequestProcessor = uniRefCompositeRequestProcessor;
        this.jobService = jobService;
    }

    @Override
    public void process(UniRefDownloadRequest request) {
        jobService.update(request.getDownloadJobId(), Map.of(JobFields.STATUS.getName(), RUNNING));
        uniRefCompositeRequestProcessor.process(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(
                        JobFields.STATUS.getName(),
                        FINISHED,
                        JobFields.RESULT_FILE.getName(),
                        request.getDownloadJobId()));
    }
}
