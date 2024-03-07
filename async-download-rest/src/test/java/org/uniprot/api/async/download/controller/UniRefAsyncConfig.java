package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class UniRefAsyncConfig {
    @Value("${async.download.uniref.result.idFilesFolder}")
    String idsFolder;

    @Value("${async.download.uniref.result.resultFilesFolder}")
    String resultFolder;

    @Value("${async.download.uniref.queueName}")
    String downloadQueue;

    @Value("${async.download.uniref.retryQueueName}")
    String retryQueue;

    @Value(("${async.download.uniref.rejectedQueueName}"))
    String rejectedQueue;
}
