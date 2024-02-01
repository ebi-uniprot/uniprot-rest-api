package org.uniprot.api.idmapping.common.queue;

import org.uniprot.api.idmapping.common.request.IdMappingDownloadRequest;

public interface IdMappingProducerMessageService {
    String sendMessage(IdMappingDownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
