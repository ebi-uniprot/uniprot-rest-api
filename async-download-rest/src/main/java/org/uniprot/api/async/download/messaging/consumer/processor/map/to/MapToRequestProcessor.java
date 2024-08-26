package org.uniprot.api.async.download.messaging.consumer.processor.map.to;

import org.uniprot.api.async.download.messaging.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Stream;

public abstract class MapToRequestProcessor<T extends MapDownloadRequest> implements IdRequestProcessor<T> {
    protected static final String TOTAL_ENTRIES = "totalEntries";
    private final MapFileHandler fileHandler;
    private final MapJobService jobService;

    protected MapToRequestProcessor(MapFileHandler fileHandler, MapJobService jobService) {
        this.fileHandler = fileHandler;
        this.jobService = jobService;
    }

    @Override
    public void process(T request) {
        writeFromIdentifiers(request, getIds(request));
    }

    private void writeFromIdentifiers(T request, Stream<String> ids) {
        updateTotalEntries(request, getSolrHits(ids));
        writeIdentifiers(request, mapIds(ids));
    }

    protected abstract long getSolrHits(Stream<String> request);

    protected abstract Stream<String> mapIds(Stream<String> downloadRequest);

    private void writeIdentifiers(T request, Stream<String> ids) {
        fileHandler.writeIds(request.getDownloadJobId(), ids);
    }

    private Stream<String> getIds(T request) {
        try {
            return Files.lines(fileHandler.getIdFile(request.getDownloadJobId()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void updateTotalEntries(T request, long totalEntries) {
        jobService.update(request.getDownloadJobId(), Map.of(TOTAL_ENTRIES, totalEntries));
    }
}
