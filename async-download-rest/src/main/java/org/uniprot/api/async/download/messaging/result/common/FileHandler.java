package org.uniprot.api.async.download.messaging.result.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;
import org.uniprot.api.async.download.messaging.consumer.heartbeat.HeartbeatProducer;
import org.uniprot.api.rest.output.context.FileType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class FileHandler {
    private final DownloadConfigProperties downloadConfigProperties;
    private final HeartbeatProducer heartbeatProducer;

    protected FileHandler(
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
                heartbeatProducer.generateForIds(jobId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            heartbeatProducer.stop(jobId);
        }
    }

    public boolean isIdFilePresent(String jobId) {
        return Files.exists(getIdFile(jobId));
    }

    public boolean isResultFilePresent(String jobId) {
        return Files.exists(getResultFile(jobId));
    }

    public boolean areAllFilesPresent(String jobId) {
        return isIdFilePresent(jobId) && isResultFilePresent(jobId);
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
            throw new FileHandlerException(e);
        }
    }
}
