package org.uniprot.api.async.download.messaging.consumer.processor.mapto;

import static org.uniprot.api.async.download.messaging.repository.JobFields.*;
import static org.uniprot.api.rest.download.model.JobStatus.FINISHED;
import static org.uniprot.api.rest.download.model.JobStatus.RUNNING;
import static org.uniprot.store.search.SolrCollection.*;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.messaging.consumer.processor.mapto.composite.UniProtKBToUniRefCompositeRequestProcessor;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.model.request.mapto.UniProtKBToUniRefDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

@Component
public class MapToRequestProcessor implements RequestProcessor<MapToDownloadRequest> {
    public static final String UNIPROT_KB = uniprot.name();
    public static final String UNIREF = uniref.name();
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
        jobService.update(request.getDownloadJobId(), Map.of(STATUS.getName(), RUNNING));
        processRequest(request);
        jobService.update(
                request.getDownloadJobId(),
                Map.of(
                        STATUS.getName(),
                        FINISHED,
                        RESULT_FILE.getName(),
                        request.getDownloadJobId()));
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