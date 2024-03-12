package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class UniRefAsyncConfig implements TestAsyncConfig {
    @Value("${async.download.uniref.retryMaxCount}")
    private int maxRetry;

    @Value("${async.download.uniref.result.idFilesFolder}")
    private String idsFolder;

    @Value("${async.download.uniref.result.resultFilesFolder}")
    private String resultFolder;

    @Value("${async.download.uniref.queueName}")
    private String downloadQueue;

    @Value("${async.download.uniref.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.uniref.rejectedQueueName}"))
    private String rejectedQueue;

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
        return maxRetry;
    }
}
