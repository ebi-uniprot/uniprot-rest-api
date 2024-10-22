package org.uniprot.api.async.download.controller;

public interface TestAsyncConfig {
    String getIdsFolder();

    String getResultFolder();

    String getFromIdsFolder();

    String getDownloadQueue();

    String getRetryQueue();

    String getRejectedQueue();

    int getMaxRetry();
}
