package org.uniprot.api.async.download.messaging.consumer.processor.mapto;

import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.mapto.UniProtKBToUniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class MapToRequestProcessor implements RequestProcessor<MapToDownloadRequest> {
    protected static final String RESULT_FILE = "resultFile";
    protected static final String STATUS = "status";
    public static final String UNIPROT_KB = "UniProtKB";
    public static final String UNIREF = "UniRef";
    private final UniProtKBToUniRefCompositeRequestProcessor
            uniProtKBToUniRefCompositeRequestProcessor;
    private final MapToJobService jobService;

    public MapToRequestProcessor(
            UniProtKBToUniRefCompositeRequestProcessor uniProtKBToUniRefCompositeRequestProcessor,
            MapToJobService jobService) {
        this.uniProtKBToUniRefCompositeRequestProcessor =
                uniProtKBToUniRefCompositeRequestProcessor;
        this.jobService = jobService;
    }

    @Override
    public void process(MapToDownloadRequest request) {
        jobService.update(request.getDownloadJobId(), Map.of(STATUS, RUNNING));
        processRequest(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(STATUS, FINISHED, RESULT_FILE, request.getDownloadJobId()));
    }

    private void processRequest(MapToDownloadRequest request) {
        if (Objects.equals(UNIPROT_KB, request.getFrom())
                && Objects.equals(UNIREF, request.getTo())) {
            uniProtKBToUniRefCompositeRequestProcessor.process(
                    (UniProtKBToUniRefDownloadRequest) request);
        } else {
            throw new IllegalArgumentException(
                    "Invalid from: %s or/and to: %s".formatted(request.getFrom(), request.getTo()));
        }
    }
}
