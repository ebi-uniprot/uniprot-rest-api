package org.uniprot.api.rest.download.queue;

import org.uniprot.api.rest.request.DownloadRequest;

/**
 * @author sahmad
 * @created 22/11/2022
 */
public interface ProducerMessageService {
    String sendMessage(DownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
