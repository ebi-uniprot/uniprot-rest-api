package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class UniProtKBAsyncConfig {
    @Value("${async.download.uniprotkb.result.idFilesFolder}")
    String idsFolder;

    @Value("${async.download.uniprotkb.result.resultFilesFolder}")
    String resultFolder;

    @Value("${async.download.uniprotkb.queueName}")
    String downloadQueue;

    @Value("${async.download.uniprotkb.retryQueueName}")
    String retryQueue;

    @Value(("${async.download.uniprotkb.rejectedQueueName}"))
    String rejectedQueue;
}
