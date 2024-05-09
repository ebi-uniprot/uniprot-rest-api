package org.uniprot.api.async.download.messaging.producer.uniprotkb;

import java.time.LocalDateTime;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.async.download.messaging.config.uniprotkb.UniProtKBRabbitTemplate;
import org.uniprot.api.async.download.messaging.producer.common.RabbitProducerMessageService;
import org.uniprot.api.async.download.messaging.repository.UniProtKBDownloadJobRepository;
import org.uniprot.api.async.download.messaging.result.uniprotkb.UniProtKBAsyncDownloadFileHandler;
import org.uniprot.api.async.download.model.common.DownloadRequest;
import org.uniprot.api.async.download.model.uniprotkb.UniProtKBDownloadJob;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service
@Slf4j
public class UniProtKBRabbitProducerMessageService extends RabbitProducerMessageService {
    private final UniProtKBDownloadJobRepository downloadJobRepository;

    public UniProtKBRabbitProducerMessageService(
            MessageConverter converter,
            UniProtKBRabbitTemplate uniProtKBRabbitTemplate,
            UniProtKBDownloadJobRepository uniProtKBDownloadJobRepository,
            HashGenerator<DownloadRequest> uniProtKBHashGenerator,
            UniProtKBAsyncDownloadSubmissionRules uniProtKBAsyncDownloadSubmissionRules,
            UniProtKBAsyncDownloadFileHandler uniProtKBAsyncDownloadFileHandler) {
        super(
                converter,
                uniProtKBRabbitTemplate,
                uniProtKBDownloadJobRepository,
                uniProtKBHashGenerator,
                uniProtKBAsyncDownloadSubmissionRules,
                uniProtKBAsyncDownloadFileHandler);
        this.downloadJobRepository = uniProtKBDownloadJobRepository;
    }

    protected void createDownloadJob(String jobId, DownloadRequest downloadRequest) {
        UniProtKBDownloadJob.UniProtKBDownloadJobBuilder jobBuilder =
                UniProtKBDownloadJob.builder();
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
