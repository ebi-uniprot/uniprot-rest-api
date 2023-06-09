package org.uniprot.api.idmapping.queue;

import org.uniprot.api.idmapping.controller.request.IdMappingDownloadRequest;

public interface IdMappingProducerMessageService {
    String sendMessage(IdMappingDownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
