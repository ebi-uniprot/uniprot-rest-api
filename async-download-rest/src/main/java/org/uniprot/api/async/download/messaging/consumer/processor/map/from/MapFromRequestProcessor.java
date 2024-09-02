package org.uniprot.api.async.download.messaging.consumer.processor.map.from;

import java.util.Map;
import java.util.stream.Stream;

import org.uniprot.api.async.download.messaging.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

public abstract class MapFromRequestProcessor<T extends MapDownloadRequest>
        implements IdRequestProcessor<T> {
    private static final String TOTAL_FROM_IDS = "totalFromIds";
    private final FileHandler fileHandler;
    private final MapJobService jobService;

    protected MapFromRequestProcessor(FileHandler fileHandler, MapJobService mapJobService) {
        this.fileHandler = fileHandler;
        this.jobService = mapJobService;
    }

    @Override
    public void process(T request) {
        updateTotalFromIds(request, getSolrHits(request.getQuery()));
        writeFromIdentifiers(request, streamIds(request));
    }

    private void updateTotalFromIds(T request, long totalFromIds) {
        jobService.update(request.getDownloadJobId(), Map.of(TOTAL_FROM_IDS, totalFromIds));
    }

    protected abstract long getSolrHits(String query);

    private void writeFromIdentifiers(T request, Stream<String> ids) {
        fileHandler.writeFromIds(request.getDownloadJobId(), ids);
    }

    protected abstract Stream<String> streamIds(T downloadRequest);
}
