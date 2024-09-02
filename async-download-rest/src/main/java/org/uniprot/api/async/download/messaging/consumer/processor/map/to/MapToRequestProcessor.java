package org.uniprot.api.async.download.messaging.consumer.processor.map.to;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Stream;

import org.uniprot.api.async.download.messaging.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.map.MapFileHandler;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;
import org.uniprot.api.async.download.service.map.MapJobService;

public abstract class MapToRequestProcessor<T extends MapDownloadRequest>
        implements IdRequestProcessor<T> {
    protected static final String TOTAL_ENTRIES = "totalEntries";
    private final MapFileHandler fileHandler;
    private final MapJobService jobService;

    protected MapToRequestProcessor(MapFileHandler fileHandler, MapJobService jobService) {
        this.fileHandler = fileHandler;
        this.jobService = jobService;
    }

    @Override
    public void process(T request) {
        writeToIdentifiers(request, getFromIds(request));
    }

    private void writeToIdentifiers(T request, Stream<String> ids) {
        String query = getQuery(ids);
        updateTotalEntries(request, getSolrHits(query));
        writeIdentifiers(request, mapIds(query));
    }

    protected abstract long getSolrHits(String query);

    protected abstract String getQuery(Stream<String> ids);

    protected abstract Stream<String> mapIds(String query);

    private void writeIdentifiers(T request, Stream<String> ids) {
        fileHandler.writeIds(request.getDownloadJobId(), ids);
    }

    private Stream<String> getFromIds(T request) {
        try {
            return Files.lines(fileHandler.getFromIdFile(request.getDownloadJobId()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void updateTotalEntries(T request, long totalEntries) {
        jobService.update(request.getDownloadJobId(), Map.of(TOTAL_ENTRIES, totalEntries));
    }
}
