package org.uniprot.api.async.download.queue.idmapping;

import org.uniprot.api.idmapping.common.request.IdMappingDownloadRequest;

public interface IdMappingProducerMessageService {
    String sendMessage(IdMappingDownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
