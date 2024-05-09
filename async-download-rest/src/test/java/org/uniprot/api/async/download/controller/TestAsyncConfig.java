package org.uniprot.api.async.download.controller;

public interface TestAsyncConfig {
    String getIdsFolder();

    String getResultFolder();

    String getDownloadQueue();

    String getRetryQueue();

    String getRejectedQueue();

    int getMaxRetry();
}
