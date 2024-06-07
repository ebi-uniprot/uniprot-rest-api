package org.uniprot.api.async.download.messaging.consumer.processor;

import org.uniprot.api.async.download.model.request.DownloadRequest;

public interface RequestProcessor<T extends DownloadRequest> {
    void process(T request);
}
