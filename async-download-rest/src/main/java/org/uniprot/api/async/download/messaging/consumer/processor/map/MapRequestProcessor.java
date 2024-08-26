package org.uniprot.api.async.download.messaging.consumer.processor.map;

import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.composite.map.UniProtKBToUniRefMapCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.model.request.map.UniProtKBMapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

@Component
public class MapRequestProcessor implements RequestProcessor<MapDownloadRequest> {
    protected static final String RESULT_FILE = "resultFile";
    protected static final String STATUS = "status";
    public static final String UNIPROT_KB = "UniProtKB";
    public static final String UNIREF = "UniRef";
    private final UniProtKBToUniRefMapCompositeRequestProcessor
            uniProtKBToUniRefMapCompositeRequestProcessor;
    private final MapJobService jobService;

    public MapRequestProcessor(
            UniProtKBToUniRefMapCompositeRequestProcessor
                    uniProtKBToUniRefMapCompositeRequestProcessor,
            MapJobService jobService) {
        this.uniProtKBToUniRefMapCompositeRequestProcessor =
                uniProtKBToUniRefMapCompositeRequestProcessor;
        this.jobService = jobService;
    }

    @Override
    public void process(MapDownloadRequest request) {
        jobService.update(request.getDownloadJobId(), Map.of(STATUS, RUNNING));
        processRequest(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(STATUS, FINISHED, RESULT_FILE, request.getDownloadJobId()));
    }

    private void processRequest(MapDownloadRequest request) {
        if (Objects.equals(UNIPROT_KB, request.getFrom())
                && Objects.equals(UNIREF, request.getTo())) {
            uniProtKBToUniRefMapCompositeRequestProcessor.process(
                    (UniProtKBMapDownloadRequest) request);
        } else {
            throw new IllegalArgumentException(
                    "Invalid from: %s or/and to: %s".formatted(request.getFrom(), request.getTo()));
        }
    }
}
