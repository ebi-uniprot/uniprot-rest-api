package org.uniprot.api.async.download.messaging.producer.idmapping;

import org.uniprot.api.async.download.model.idmapping.IdMappingDownloadRequest;

public interface IdMappingProducerMessageService {
    String sendMessage(IdMappingDownloadRequest downloadRequest);

    void alreadyProcessed(String jobId);
}
