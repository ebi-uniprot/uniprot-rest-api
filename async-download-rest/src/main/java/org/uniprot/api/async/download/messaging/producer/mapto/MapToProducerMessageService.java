package org.uniprot.api.async.download.messaging.producer.mapto;

import java.time.LocalDateTime;

import org.springframework.amqp.support.converter.MessageConverter;
import org.uniprot.api.async.download.messaging.producer.SolrProducerMessageService;
import org.uniprot.api.async.download.messaging.result.mapto.MapToFileHandler;
import org.uniprot.api.async.download.model.job.mapto.MapToDownloadJob;
import org.uniprot.api.async.download.model.request.mapto.MapToDownloadRequest;
import org.uniprot.api.async.download.mq.mapto.MapToMessagingService;
import org.uniprot.api.async.download.service.mapto.MapToJobService;
import org.uniprot.api.rest.download.model.JobStatus;
import org.uniprot.api.rest.request.HashGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MapToProducerMessageService<T extends MapToDownloadRequest>
        extends SolrProducerMessageService<T, MapToDownloadJob> {
    private final MapToJobService jobService;

    protected MapToProducerMessageService(
            MapToJobService jobService,
            MessageConverter messageConverter,
            MapToMessagingService messagingService,
            HashGenerator<T> hashGenerator,
            MapToFileHandler asyncDownloadFileHandler,
            MapToJobSubmissionRules<T> asyncDownloadSubmissionRules) {
        super(
                jobService,
                messageConverter,
                messagingService,
                hashGenerator,
                asyncDownloadFileHandler,
                asyncDownloadSubmissionRules);
        this.jobService = jobService;
    }

    @Override
    protected void createDownloadJob(String jobId, T request) {
        MapToDownloadJob.MapDownloadJobBuilder jobBuilder = MapToDownloadJob.builder();
        LocalDateTime now = LocalDateTime.now();
        jobBuilder.id(jobId).status(JobStatus.NEW);
        jobBuilder
                .query(request.getQuery())
                .fields(request.getFields())
                .sort(request.getSort())
                .format(request.getFormat())
                .created(now)
                .updated(now);
        jobService.save(jobBuilder.build());
        log.info("Job with jobId {} created in redis", jobId);
    }
}
