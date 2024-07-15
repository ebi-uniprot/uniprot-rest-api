package org.uniprot.api.async.download.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class UniParcAsyncConfig implements TestAsyncConfig {
    @Value("${async.download.uniparc.retryMaxCount}")
    private int maxRetry;

    @Value("${async.download.uniparc.result.idFilesFolder}")
    private String idsFolder;

    @Value("${async.download.uniparc.result.resultFilesFolder}")
    private String resultFolder;

    @Value("${async.download.uniparc.queueName}")
    private String downloadQueue;

    @Value("${async.download.uniparc.retryQueueName}")
    private String retryQueue;

    @Value(("${async.download.uniparc.rejectedQueueName}"))
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
