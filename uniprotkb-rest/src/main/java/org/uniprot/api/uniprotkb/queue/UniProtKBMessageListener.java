package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.download.model.DownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.rest.download.repository.DownloadJobRepository;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service("Consumer")
@Slf4j
public class UniProtKBMessageListener implements MessageListener {

    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;

    private DownloadJobRepository jobRepository;

    public UniProtKBMessageListener(
            MessageConverter converter,
            UniProtEntryService service,
            DownloadConfigProperties downloadConfigProperties,
            DownloadJobRepository jobRepository) {
        this.converter = converter;
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
        this.jobRepository = jobRepository;
    }

    @Override
    public void onMessage(Message message) {
        UniProtKBStreamRequest request = (UniProtKBStreamRequest) converter.fromMessage(message);
        String jobId = message.getMessageProperties().getHeader("jobId");
        Optional<DownloadJob> optDownloadJob = this.jobRepository.findById(jobId);
        if (optDownloadJob.isEmpty()) {
            // TODO handle error
        }
        DownloadJob downloadJob = optDownloadJob.get();
        downloadJob.setStatus(JobStatus.RUNNING);
        Path idsFile = Paths.get(downloadConfigProperties.getFolder(), jobId);
        if (Files.notExists(idsFile)) {
            updateDownloadJob(downloadJob, JobStatus.RUNNING);
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            updateDownloadJob(downloadJob, JobStatus.FINISHED);
        } else {
            // redis update status?
        }

        // TESTING TO ALSO CREATE RESULT
        log.info("Message processed");
        // talk to redis
        // talk to solr
        // write to nfs
        // acknowledge the queue with failure/success
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) {
        Iterable<String> source = ids::iterator;
        try {
            Files.write(filePath, source, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Stream<String> streamIds(UniProtKBStreamRequest request) {
        return service.streamIds(request);
    }

    private void updateDownloadJob(DownloadJob downloadJob, JobStatus jobStatus) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        downloadJob.setUpdated(now);
        downloadJob.setStatus(jobStatus);
        this.jobRepository.save(downloadJob);
    }
}
