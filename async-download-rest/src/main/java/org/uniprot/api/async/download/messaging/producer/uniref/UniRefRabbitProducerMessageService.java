package org.uniprot.api.async.download.messaging.producer.uniref;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.config.uniref.UniRefRabbitTemplate;
import org.uniprot.api.async.download.messaging.producer.common.RabbitProducerMessageService;
import org.uniprot.api.async.download.messaging.repository.UniRefDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniref.UniRefAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.async.download.model.uniref.UniRefDownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service
@Slf4j
public class UniRefRabbitProducerMessageService extends RabbitProducerMessageService {

    private final UniRefDownloadJobRepository downloadJobRepository;

    public UniRefRabbitProducerMessageService(
            MessageConverter converter,
            UniRefRabbitTemplate uniRefRabbitTemplate,
            UniRefDownloadJobRepository downloadJobRepository,
            HashGenerator<DownloadRequest> uniRefHashGenerator,
            UniRefAsyncDownloadSubmissionRules uniRefAsyncDownloadSubmissionRules,
            UniRefAsyncDownloadFileHandler uniRefAsyncDownloadFileHandler) {
        super(
                converter,
                uniRefRabbitTemplate,
                downloadJobRepository,
                uniRefHashGenerator,
                uniRefAsyncDownloadSubmissionRules,
                uniRefAsyncDownloadFileHandler);
        this.downloadJobRepository = downloadJobRepository;
    }

    @Override
    protected void createDownloadJob(String jobId, DownloadRequest downloadRequest) {
        UniRefDownloadJob.UniRefDownloadJobBuilder jobBuilder = UniRefDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(downloadRequest.getQuery())
                .fields(downloadRequest.getFields())
                .sort(downloadRequest.getSort())
                .format(downloadRequest.getFormat())
                .created(now)
                .updated(now);
        this.downloadJobRepository.save(jobBuilder.build());
    }
}
