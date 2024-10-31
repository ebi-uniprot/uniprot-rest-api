package org.uniprot.api.async.download.messaging.consumer.processor.mapto.from;

import static org.uniprot.api.async.download.messaging.repository.JobFields.TOTAL_FROM_IDS;

import java.util.Map;
import java.util.stream.Stream;

import org.uniprot.api.async.download.messaging.consumer.processor.IdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.service.mapto.MapToJobService;

public abstract class MapToFromIdRequestProcessor<T extends MapToDownloadRequest>
        implements IdRequestProcessor<T> {
    private final FileHandler fileHandler;
    private final MapToJobService jobService;

    protected MapToFromIdRequestProcessor(
            FileHandler fileHandler, MapToJobService mapToJobService) {
        this.fileHandler = fileHandler;
        this.jobService = mapToJobService;
    }

    @Override
    public void process(T request) {
        updateTotalFromIds(request, getSolrHits(request.getQuery()));
        writeFromIds(request, streamIds(request));
    }

    private void updateTotalFromIds(T request, long totalFromIds) {
        jobService.update(
                request.getDownloadJobId(), Map.of(TOTAL_FROM_IDS.getName(), totalFromIds));
    }

    protected abstract long getSolrHits(String query);

    private void writeFromIds(T request, Stream<String> ids) {
        fileHandler.writeFromIds(request.getDownloadJobId(), ids);
    }

    protected abstract Stream<String> streamIds(T downloadRequest);
}
