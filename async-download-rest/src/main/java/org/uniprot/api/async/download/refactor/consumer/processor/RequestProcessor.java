package org.uniprot.api.async.download.refactor.consumer.processor;

import org.uniprot.api.async.download.refactor.request.DownloadRequest;

public interface RequestProcessor<T extends DownloadRequest> {
    void process(T request);
}
