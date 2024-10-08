package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class IdMappingAsyncConfig implements TestAsyncConfig {
    @Value("${async.download.idmapping.queueName}")
    private String downloadQueue;

    @Value("${async.download.idmapping.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.idmapping.rejectedQueueName}"))
    private String rejectedQueue;

    @Value("${async.download.idmapping.result.idFilesFolder}")
    private String idsFolder;

    @Value("${async.download.idmapping.result.fromIdFilesFolder}")
    private String fromIdsFolder;

    @Value("${async.download.idmapping.result.resultFilesFolder}")
    private String resultFolder;

    @Override
    public String getFromIdsFolder() {
        return fromIdsFolder;
    }

    @Override
    public String getIdsFolder() {
        return idsFolder;
    }

    @Override
    public String getResultFolder() {
        return resultFolder;
    }

    @Override
    public String getDownloadQueue() {
        return downloadQueue;
    }

    @Override
    public String getRetryQueue() {
        return retryQueue;
    }

    @Override
    public String getRejectedQueue() {
        return rejectedQueue;
    }

    @Override
    public int getMaxRetry() {
        return 0;
    }
}
