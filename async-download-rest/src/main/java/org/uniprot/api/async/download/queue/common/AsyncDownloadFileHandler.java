package org.uniprot.api.async.download.queue.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;

import org.uniprot.api.rest.output.context.FileType;

@Slf4j
public abstract class AsyncDownloadFileHandler {
    private final DownloadConfigProperties downloadConfigProperties;

    public AsyncDownloadFileHandler(DownloadConfigProperties downloadConfigProperties) {
        this.downloadConfigProperties = downloadConfigProperties;
    }

    public void deleteIdFile(String jobId) {
        Path idsFile = Paths.get(downloadConfigProperties.getIdFilesFolder(), jobId);
        deleteFile(idsFile, jobId);
    }

    public void deleteResultFile(String jobId) {
        String resultFileName = jobId + "." + FileType.GZIP.getExtension();
        Path resultFile =
                Paths.get(downloadConfigProperties.getResultFilesFolder(), resultFileName);
        deleteFile(resultFile, jobId);
    }

    public void deleteAllFiles(String jobId) {
        deleteIdFile(jobId);
        deleteResultFile(jobId);
    }

    private void deleteFile(Path file, String jobId) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn(
                    "Unable to delete file {} during IOException failure for job id {}",
                    file.toFile().getName(),
                    jobId);
            throw new MessageListenerException(e);
        }
    }
}
