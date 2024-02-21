package org.uniprot.api.async.download.queue.common;

import org.uniprot.api.rest.request.DownloadRequest;

/**
 * @author sahmad
 * @created 22/11/2022
 */
public interface ProducerMessageService {
    String sendMessage(DownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
