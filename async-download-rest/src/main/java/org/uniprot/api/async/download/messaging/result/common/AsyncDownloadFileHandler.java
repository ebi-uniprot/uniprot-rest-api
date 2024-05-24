package org.uniprot.api.async.download.messaging.result.common;

import lombok.extern.slf4j.Slf4j;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.listener.common.HeartbeatProducer;
import org.uniprot.api.rest.output.context.FileType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

@Slf4j
public abstract class AsyncDownloadFileHandler {
    private final DownloadConfigProperties downloadConfigProperties;
    private final HeartbeatProducer heartbeatProducer;

    protected AsyncDownloadFileHandler(
            DownloadConfigProperties downloadConfigProperties,
            HeartbeatProducer heartbeatProducer) {
        this.downloadConfigProperties = downloadConfigProperties;
        this.heartbeatProducer = heartbeatProducer;
    }

    public void writeIds(String jobId, Stream<String> ids) {
        Path idsFile = getIdFile(jobId);
        try (BufferedWriter writer =
                Files.newBufferedWriter(
                        idsFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            Iterable<String> iterator = ids::iterator;
            for (String id : iterator) {
                writer.append(id);
                writer.newLine();
                heartbeatProducer.createForIds(jobId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            heartbeatProducer.stop(jobId);
        }
    }

    public boolean isIdFileExist(String jobId) {
        return Files.exists(getIdFile(jobId));
    }

    public boolean isResultFileExist(String jobId) {
        return Files.exists(getResultFile(jobId));
    }

    public Path getIdFile(String jobId) {
        return getPath(downloadConfigProperties.getIdFilesFolder(), jobId);
    }

    public Path getResultFile(String jobId) {
        String resultFileName = jobId + "." + FileType.GZIP.getExtension();
        return getPath(downloadConfigProperties.getResultFilesFolder(), resultFileName);
    }

    private Path getPath(String folder, String fileName) {
        return Paths.get(folder, fileName);
    }

    public void deleteIdFile(String jobId) {
        Path idsFile = getIdFile(jobId);
        deleteFile(idsFile, jobId);
    }

    public void deleteResultFile(String jobId) {
        deleteFile(getResultFile(jobId), jobId);
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
            throw new FileHandelerException(e);
        }
    }
}
