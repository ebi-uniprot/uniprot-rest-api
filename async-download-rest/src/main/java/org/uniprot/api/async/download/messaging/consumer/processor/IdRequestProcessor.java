package org.uniprot.api.async.download.messaging.consumer.processor;

import org.uniprot.api.async.download.model.request.DownloadRequest;

public interface IdRequestProcessor<T extends DownloadRequest> extends RequestProcessor<T> {}