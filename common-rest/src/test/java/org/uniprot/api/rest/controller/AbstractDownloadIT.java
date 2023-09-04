package org.uniprot.api.rest.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDownloadIT extends AbstractStreamControllerIT {

    @Value("${download.idFilesFolder}")
    protected String idsFolder;

    @Value("${download.resultFilesFolder}")
    protected String resultFolder;

    @Value("${async.download.queueName}")
    protected String downloadQueue;

    @Value("${async.download.retryQueueName}")
    protected String retryQueue;

    @Value(("${async.download.rejectedQueueName}"))
    protected String rejectedQueue;

    protected void prepareDownloadFolders() throws IOException {
        Files.createDirectories(Path.of(this.idsFolder));
        Files.createDirectories(Path.of(this.resultFolder));
    }

    protected abstract DownloadJobRepository getDownloadJobRepository();

    protected abstract void saveEntries() throws Exception;

    protected void cleanUpFolder(String folder) throws IOException {
        Files.list(Path.of(folder))
                .forEach(
                        path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }
}
