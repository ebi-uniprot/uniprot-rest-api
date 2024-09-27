package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class MapAsyncConfig implements TestAsyncConfig {
    @Value("${async.download.map.retryMaxCount}")
    private int maxRetry;

    @Value("${async.download.map.result.idFilesFolder}")
    private String idsFolder;

    @Value("${async.download.map.result.fromIdFilesFolder}")
    private String fromIdsFolder;

    @Value("${async.download.map.result.resultFilesFolder}")
    private String resultFolder;

    @Value("${async.download.map.queueName}")
    private String downloadQueue;

    @Value("${async.download.map.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.mapto.rejectedQueueName}"))
    private String rejectedQueue;

    @Override
    public String getIdsFolder() {
        return idsFolder;
    }

    @Override
    public String getFromIdsFolder() {
        return fromIdsFolder;
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
        return maxRetry;
    }
}
