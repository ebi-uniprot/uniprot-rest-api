package org.uniprot.api.async.download.refactor.consumer.processor.id;

import org.uniprot.api.async.download.refactor.consumer.processor.RequestProcessor;
import org.uniprot.api.async.download.refactor.request.DownloadRequest;

public interface IdRequestProcessor<T extends DownloadRequest> extends RequestProcessor<T> {}
