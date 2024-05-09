package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class UniProtKBAsyncConfig implements TestAsyncConfig {
    @Value("${async.download.uniprotkb.result.idFilesFolder}")
    private String idsFolder;

    @Value("${async.download.uniprotkb.result.resultFilesFolder}")
    private String resultFolder;

    @Value("${async.download.uniprotkb.queueName}")
    private String downloadQueue;

    @Value("${async.download.uniprotkb.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.uniprotkb.rejectedQueueName}"))
    private String rejectedQueue;

    @Value("${async.download.uniprotkb.retryMaxCount}")
    private int maxRetry;

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
