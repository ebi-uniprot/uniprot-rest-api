package org.uniprot.api.async.download.messaging.consumer.processor.map.from;

import org.uniprot.api.async.download.messaging.consumer.processor.id.IdRequestProcessor;
import org.uniprot.api.async.download.messaging.result.common.FileHandler;
import org.uniprot.api.async.download.model.request.map.MapDownloadRequest;

import java.util.stream.Stream;

public abstract class MapFromRequestProcessor<T extends MapDownloadRequest> implements IdRequestProcessor<T> {
    private final FileHandler fileHandler;

    protected MapFromRequestProcessor(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @Override
    public void process(T request) {
        writeFromIdentifiers(request, streamIds(request));
    }

    private void writeFromIdentifiers(T request, Stream<String> ids) {
        fileHandler.writeFromIds(request.getDownloadJobId(), ids);
    }

    protected abstract Stream<String> streamIds(T downloadRequest);
}
