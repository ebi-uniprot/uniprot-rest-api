package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class IdMappingAsyncConfig {
    @Value("${async.download.idmapping.queueName}")
    String downloadQueue;

    @Value("${async.download.idmapping.retryQueueName}")
    String retryQueue;

    @Value(("${async.download.idmapping.rejectedQueueName}"))
    String rejectedQueue;

    @Value("${async.download.idmapping.result.idFilesFolder}")
    String idsFolder;

    @Value("${async.download.idmapping.result.resultFilesFolder}")
    String resultFolder;
}
